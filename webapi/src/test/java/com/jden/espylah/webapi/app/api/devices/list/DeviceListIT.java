package com.jden.espylah.webapi.app.api.devices.list;

import com.jden.espylah.webapi.app.api.BaseAppTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DeviceListIT extends BaseAppTest {

    public DeviceListIT(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    @WithMockUser("alice@test.com")
    void returnsAllDevicesForAuthenticatedUser() throws Exception {
        getMockMvc().perform(get("/api/devices"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    @WithMockUser("alice@test.com")
    void filtersByNameCaseInsensitive() throws Exception {
        getMockMvc().perform(get("/api/devices").param("name", "shed"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Shed Hive"));
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    @WithMockUser("alice@test.com")
    void filtersByState() throws Exception {
        getMockMvc().perform(get("/api/devices").param("state", "PROVISIONED"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    @WithMockUser("alice@test.com")
    void filtersByEnabled() throws Exception {
        getMockMvc().perform(get("/api/devices").param("enabled", "false"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Orchard Trap"));
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    @WithMockUser("alice@test.com")
    void filtersByRunMode() throws Exception {
        getMockMvc().perform(get("/api/devices").param("runMode", "DEFAULT"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Garden Monitor"));
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    @WithMockUser("alice@test.com")
    void defaultSortIsCreatedAtDescending() throws Exception {
        getMockMvc().perform(get("/api/devices"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Orchard Trap"))
                .andExpect(jsonPath("$.content[2].name").value("Shed Hive"));
    }

    @Test
    @Sql(scripts = "/h2-init.sql")
    @WithMockUser("alice@test.com")
    void respectsPageSize() throws Exception {
        getMockMvc().perform(get("/api/devices").param("size", "2").param("page", "0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }
}
