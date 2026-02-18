package com.prodsight.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = com.prodsight.api.ProdsightApplication.class,
properties = {
  "spring.autoconfigure.exclude=" +
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
}
)
class ProdsightApplicationTests {

	void contextLoads() {
	}

}
