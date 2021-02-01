package com.myagent.web.controller;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@RestController
@RequestMapping("/resource")
public class ResourceController {

    @Value(value = "${resource.path:.}")
    private String resourcePath;

    @GetMapping("/class1/{name}")
    public void getClazz(@PathVariable String name, HttpServletResponse response) {
        File file = null;
        file = new File(resourcePath + "/" + name.replace(".", "/"));
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file); OutputStream out = response.getOutputStream()) {
            IOUtils.copy(fileInputStream, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
