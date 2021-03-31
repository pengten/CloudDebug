package com.myagent.web.controller;

import com.myagent.web.vm.VirtualMachineManager;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Location;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/agent")
public class SendAgentController {

    private static final String URL_FORMART = "http://%s:%s/resource/class/%s";

    @Value("${server.host:localhost}")
    private String host;

    @Value("${server.port:80}")
    private String port;

    @Value("${java.library.path}")
    private String libPath;


    @GetMapping("/send")
    public void sendAgent(@RequestParam String className, @RequestParam String lines) throws AbsentInformationException {
        VirtualMachine virtualMachine = VirtualMachineManager.attach("emcooperate-5.gz00b.dev.alipay.net", "8000");

        EventRequestManager eventRequestManager = virtualMachine.eventRequestManager();
        ClassType clazz = (ClassType) virtualMachine.classesByName(className).get(0);
        List<Location> locations = clazz.allLineLocations();
        String[] lineArr = lines.split(",");
        locations.stream()
                .filter(location -> Arrays.asList(lineArr).contains(Integer.toString(location.lineNumber())))
                .forEach(location -> {
                    BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
                    breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                    breakpointRequest.enable();
                });
    }


}
