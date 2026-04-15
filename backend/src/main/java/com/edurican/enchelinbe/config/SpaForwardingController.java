package com.edurican.enchelinbe.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {

    // 확장자 없는 GET 경로 = SPA 라우트로 간주 → index.html forward
    @RequestMapping(value = {
        "/",
        "/{path:[^.]*}",
        "/{x:[^.]*}/{y:[^.]*}",
        "/{x:[^.]*}/{y:[^.]*}/{z:[^.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
