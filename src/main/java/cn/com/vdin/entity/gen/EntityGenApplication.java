package cn.com.vdin.entity.gen;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static cn.com.vdin.entity.gen.StringExtUtils.*;
import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@RestController
@EnableSwagger2
@Slf4j
public class EntityGenApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(EntityGenApplication.class, args);
    }

    @Autowired
    ServerProperties serverProperties;

    @Bean
    public CommandLineRunner commandLineRunner() {
        return (String[] args) -> {
            try {
                URI uri = new URI("http://localhost:" + serverProperties.getPort());
                log.info(uri.toString());
                Desktop.getDesktop().browse(uri);
            } catch (Exception e) {
                log.warn("无法打开浏览器", e);
            }
        };
    }

    private Connection getConn(String url, String userName, String password) throws SQLException {
        Driver driver = null;
        if (url.contains("mysql")) {
            driver = new com.mysql.jdbc.Driver();
        } else if (url.contains("postgresql")) {
            driver = new org.postgresql.Driver();
        } else if (url.contains("sqlserver")) {
            driver = new com.microsoft.sqlserver.jdbc.SQLServerDriver();
        } else if (url.contains("oracle")) {
            driver = new oracle.jdbc.OracleDriver();
        } else {
            throw new RuntimeException("不支持此类型数据库");
        }

        DataSource dataSource = new SimpleDriverDataSource(driver, url, userName, password);
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value = "get_tables")
    public List<String> getTables(@RequestParam String url,
                                  @RequestParam String userName,
                                  @RequestParam String password) throws SQLException {
        Connection connection = getConn(url, userName, password);
        ResultSet resultSet = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"});

        List<String> tables = new ArrayList<>();
        while (resultSet.next()) {
            String table_name = resultSet.getString("TABLE_NAME");
            tables.add(table_name);
        }
        connection.close();
        return tables;
    }


    @GetMapping(value = "get_class")
    public Map<String, List<String>> getCls(@RequestParam List<String> tableNames,
                                            @RequestParam String url,
                                            @RequestParam String userName,
                                            @RequestParam(required = false) String pkgName,
                                            @RequestParam String password) throws SQLException {
        log.warn("111");
        Connection conn = getConn(url, userName, password);
        Map<String, List<String>> nameWithJavaMap = getStringListMap(tableNames, conn, pkgName);
        conn.close();
        return nameWithJavaMap;
    }

    @GetMapping(value = "get_class_file")
    public void getClassFile(@RequestParam List<String> tableNames,
                             @RequestParam String url,
                             @RequestParam String userName,
                             @RequestParam String password,
                             @RequestParam(required = false) String pkgName,
                             HttpServletResponse response) throws SQLException, IOException {
        Connection conn = getConn(url, userName, password);
        Map<String, List<String>> nameWithJavaMap = getStringListMap(tableNames, conn, pkgName);
        conn.close();

        ServletOutputStream outputStream = response.getOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);


        for (Map.Entry<String, List<String>> stringListEntry : nameWithJavaMap.entrySet()) {
            zipOutputStream.putNextEntry(
                    new ZipEntry(
                            getJavaClassName(stringListEntry.getKey()) + ".java"
                    )
            );
            for (String line : stringListEntry.getValue()) {
                zipOutputStream.write(line.getBytes());
                zipOutputStream.write('\n');
            }
        }
        response.setContentType("application/zip;charset=utf-8");
        response.setHeader("Content-disposition", "attachment;filename= " + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".zip");
        zipOutputStream.close();

    }

    private Map<String, List<String>> getStringListMap(List<String> tableNames,
                                                       Connection conn, String pkgName) throws SQLException {
        DatabaseMetaData connMetaData = conn.getMetaData();

        Map<String, List<String>> nameWithJavaMap = new HashMap<>();

        for (String tableName : tableNames) {
            ResultSet resultSet = connMetaData.getColumns(null, "%", tableName, "%");

            String entityName = getJavaClassName(tableName);
            List<String> javaClassStr = new ArrayList<>();
            if (!StringUtils.isEmpty(pkgName)) {
                javaClassStr.add("package " + pkgName + ";");
                javaClassStr.add("");
            }
            javaClassStr.add("import javax.persistence.*;");
            javaClassStr.add("import java.time.*;");
            javaClassStr.add("import java.util.*;");
            javaClassStr.add("import java.math.*;");
            javaClassStr.add("import lombok.*;");
            javaClassStr.add("");
            javaClassStr.add("@Getter");
            javaClassStr.add("@Setter");
            javaClassStr.add("@Entity");
            javaClassStr.add("@Table(name = \"" + tableName + "\")");
            javaClassStr.add("public class " + entityName + " {");


            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                String columnType = resultSet.getString("TYPE_NAME");
                if (columnName.equals("id")) {
                    javaClassStr.add("    @Id");
                }
                if (isFirstLowerCamel(columnName)) {
                    javaClassStr.add("    private " + getJavaType(columnType) + " " + columnName + ";");
                } else {
                    javaClassStr.add("    private " + getJavaType(columnType) + " " + underlineToCamel(columnName) + ";");
                }
                javaClassStr.add("");

                System.out.println(columnName + "  " + columnType);
            }
            javaClassStr.add("}");
            nameWithJavaMap.put(tableName, javaClassStr);
        }
        return nameWithJavaMap;
    }

    private String getJavaClassName(String tableName) {
        return pluralToSingular(firstCharUpper(underlineToCamel(tableName)));
    }


    private String getJavaType(String columnType) {
        columnType = columnType.toLowerCase();
        if (columnType.contains("int")) {
            return "Integer";
        } else if (columnType.contains("bool")) {
            return "Boolean";
        } else if (columnType.contains("decimal") || columnType.contains("real")) {
            return "BigDecimal";
        } else if (columnType.contains("float") || columnType.contains("double")) {
            return "Double";
        } else if (columnType.contains("datetime") || columnType.contains("timestamp")) {
            return "LocalDateTime";
        } else if (columnType.contains("date")) {
            return "LocalDate";
        } else if (columnType.contains("time")) {
            return "LocalTime";
        } else {
            return "String";
        }
    }

    @Bean
    public Docket customDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
//                .apis(not(RequestHandlerSelectors.basePackage("org.springframework.boot.autoconfigure.web")))
                .apis(RequestHandlerSelectors.basePackage("cn.com.vdin.entity.gen"))//*不同项目此处需要修改
                .paths(or(regex("/.*")))
                .build()//--------------
                .directModelSubstitute(LocalDate.class, String.class)
                .directModelSubstitute(LocalDateTime.class, String.class)
                .apiInfo(getApiInfo()) //以下为文档信息设置
                .produces(Sets.newHashSet("application/json"))
                .consumes(Sets.newHashSet("application/json"));
    }

    private ApiInfo getApiInfo() {
        return new ApiInfoBuilder()
                .title("主平台")
                .version("2.0")
                .description("主平台practition-api")
                .contact(new Contact("zdht", "http://www.vdin.com.cn/", "zdht@vdin.net"))
                .build();
    }
}
