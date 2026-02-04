package com.umdev.directory.api.v1.controller;

import com.umdev.directory.api.v1.service.UserRegisterService;
import com.umdev.directory.api.v1.to.ConfirmCodeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class UserRegisterControllerTest {

    @Mock
    private UserRegisterService userRegisterService;

    @InjectMocks
    private UserRegisterController controller;

    @Test
    @DisplayName("confirmCode returns OK on success")
    void confirm_ok() {
        when(userRegisterService.confirmCode(anyString(), any(ConfirmCodeRequest.class))).thenReturn(ResponseEntity.ok().build());
        var req = new ConfirmCodeRequest(UUID.randomUUID(), "123456");
        var resp = controller.confirmCode("token", req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("confirmCode returns BAD_REQUEST on invalid input")
    void confirm_badRequest() {
        when(userRegisterService.confirmCode(anyString(), any(ConfirmCodeRequest.class))).thenReturn(ResponseEntity.badRequest().build());
        var req = new ConfirmCodeRequest(UUID.randomUUID(), "");
        var resp = controller.confirmCode("token", req);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}

