package com.camunda.training.twitterQa;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.*;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ArquillianTest {
  
  private static final String PROCESS_DEFINITION_KEY = "twitter-qa";

  @Deployment
  public static WebArchive createDeployment() {
    // resolve given dependencies from Maven POM
    File[] libs = Maven.resolver()
      .offline(false)
      .loadPomFromFile("pom.xml")
      .importRuntimeAndTestDependencies().resolve().withTransitivity().asFile();

    return ShrinkWrap
            .create(WebArchive.class, "twitter-qa.war")
            // add needed dependencies
            .addAsLibraries(libs)
            // prepare as process application archive for camunda BPM Platform
            .addAsResource("META-INF/processes.xml", "META-INF/processes.xml")
            // enable CDI
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            // boot JPA persistence unit
            .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
            // add your own classes (could be done one by one as well)
            .addPackages(false, "com.camunda.training.twitterQa") // not recursive to skip package 'nonarquillian'
            // add process definition
            .addAsResource("process.bpmn")
            // add process image for visualizations
            .addAsResource("process.png")
            // now you can add additional stuff required for your test case
    ;
  }

  @Inject
  private ProcessEngine processEngine;

  @Before
  public void setup() {
	init(processEngine);
  }

  @Test
  public void testRunProcessWithPublish() {
    cleanUpRunningProcessInstances();
    ProcessInstance pi = runtimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY, 
        withVariables("content", "I did it with variable and user task from Arquillian! Cheers The Trainer"));
    Task reviewTask = taskQuery().taskCandidateGroup("management").singleResult();
    assertThat(reviewTask).hasName("Review Tweet");
    taskService().complete(reviewTask.getId(), withVariables("approved", true));
    assertThat(pi).isEnded().hasPassed("ServiceTask_1");
  }
  

  /**
   * Helper to delete all running process instances, which might disturb our Arquillian Test case
   * Better run test cases in a clean environment, but this is pretty handy for demo purposes
   */
  private void cleanUpRunningProcessInstances() {
    List<ProcessInstance> runningInstances = processInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).list();
    for (ProcessInstance processInstance : runningInstances) {
      runtimeService().deleteProcessInstance(processInstance.getId(), "deleted to have a clean environment for Arquillian");
    }
  }  
}