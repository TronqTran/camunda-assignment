package com.example.workflow.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfirmOrderTaskListener implements TaskListener {
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public void notify(DelegateTask delegateTask) {

    }
}
