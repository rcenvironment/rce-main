Feature: ExampleWorkflows

# executing this scenario should be possible after the next major update (RCE 11.0), since the base version then has the necessary commands to export wf runs
#@ExampleWorkflow01 
#@NoGUITestSuite
#@BasicIntegrationTestSuite
#Scenario: Workflow Data maintained different versions
#
#	Given instance "NodeA" using the default build
#	
#	When  starting instance "NodeA"
#	And   executing workflows "example_workflows\\01_01_Hello_World.wf, example_workflows\\01_02_Coupling_Components.wf, example_workflows\\01_03_Data_Types.wf, example_workflows\\01_04_Component_Execution_Scheduling.wf, example_workflows\\02_02_Evaluation_Drivers.wf, example_workflows\\03_01_Simple_Loop.wf, example_workflows\\03_02_Forwarding_Values.wf, example_workflows\\03_03_Nested_Loop.wf, example_workflows\\03_04_Fault-tolerant_Loop.wf" on "NodeA"
#	And   exporting all workflow runs from "NodeA" to "base"
#	And   stopping instance "NodeA"
#	
#	Given the same instance "NodeA" using the default build
#	
#	When  starting instance "NodeA"
#	And   exporting all workflow runs from "NodeA" to "default"
#	
#	Then  all exported workflow run directories from "NodeA" should be identical

# workflow 2.3 (Workflow Examples Project\\02_Component Groups\\02_03_XML_Components.wf [02_03_XML_Components.json]) is not tested as it produces errors.

#@ExampleWorkflowsFeature
#@ExampleWorkflow02
#@NoGUITestSuite
#@BasicIntegrationTestSuite
#Scenario: Execute all example workflows
#
#    Given instance "NodeA" using the default build
#    
#    When  starting instance "NodeA"
#    And   adding tool "common/Example" to "NodeA"
#    And   executing workflows "Workflow Examples Project/01_First Steps/01_01_Hello_World.wf, Workflow Examples Project/01_First Steps/01_02_Coupling_Components.wf, Workflow Examples Project/01_First Steps/01_03_Data_Types.wf, Workflow Examples Project/01_First Steps/01_04_Component_Execution_Scheduling.wf, Workflow Examples Project/02_Component Groups/02_01_Data_Flow.wf [02_01_Data_Flow.json], Workflow Examples Project/02_Component Groups/02_02_Evaluation_Drivers.wf, Workflow Examples Project/02_Component Groups/02_03_XML_Components.wf [02_03_XML_Components.json], Workflow Examples Project/02_Component Groups/02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json], Workflow Examples Project/03_Workflow Logic/03_01_Simple_Loop.wf, Workflow Examples Project/03_Workflow Logic/03_02_Forwarding_Values.wf, Workflow Examples Project/03_Workflow Logic/03_03_Nested_Loop.wf, Workflow Examples Project/03_Workflow Logic/03_04_Fault-tolerant_Loop.wf, Workflow Examples Project/04_Tool Integration/04_01_Example_Integration.wf, Workflow Examples Project/04_Tool Integration/04_02_Script_And_Tool_Integration_API.wf" on "NodeA"
#    And   stopping instance "NodeA"
#
#    Then   the log output of "NodeA" should contain the pattern "(?:de.rcenvironment.core.component.api.ComponentException: Script execution error: Exception: Example failure in <script> at line number 2)"
#    And    the log output of "NodeA" should contain 1 error

@ExampleWorkflowsFeature
@ExampleWorkflow02
@NoGUITestSuite
@BasicIntegrationTestSuite
@ExampleWorkflow0201
Scenario: Execute example workflows of the group 01_First Steps

    Given instance "NodeA1" using the default build
    
    When  starting instance "NodeA1"
    And   executing workflows "Workflow Examples Project/01_First Steps/01_01_Hello_World.wf, Workflow Examples Project/01_First Steps/01_02_Coupling_Components.wf, Workflow Examples Project/01_First Steps/01_03_Data_Types.wf, Workflow Examples Project/01_First Steps/01_04_Component_Execution_Scheduling.wf" on "NodeA1"
    And   stopping instance "NodeA1"

    Then  the log output of "NodeA1" should indicate a clean shutdown with no warnings or errors
    
@ExampleWorkflowsFeature
@ExampleWorkflow02
@NoGUITestSuite
@BasicIntegrationTestSuite
@ExampleWorkflow0202
Scenario: Execute example workflows of the group 02_Component Groups

    Given instance "NodeA2" using the default build
    
    When  starting instance "NodeA2"
    And   executing workflows "Workflow Examples Project/02_Component Groups/02_01_Data_Flow.wf [02_01_Data_Flow.json], Workflow Examples Project/02_Component Groups/02_02_Evaluation_Drivers.wf, Workflow Examples Project/02_Component Groups/02_03_XML_Components.wf [02_03_XML_Components.json], Workflow Examples Project/02_Component Groups/02_04_EvaluationMemory.wf [02_04_EvaluationMemory.json]" on "NodeA2"
    And   waiting for 60 seconds
    And   stopping instance "NodeA2"

    Then  the log output of "NodeA2" should indicate a clean shutdown with no warnings or errors

@ExampleWorkflowsFeature
@ExampleWorkflow02
@NoGUITestSuite
@BasicIntegrationTestSuite
@ExampleWorkflow0203
Scenario: Execute example workflows of the group 03_Workflow Logic

    Given instance "NodeA3" using the default build
    
    When  starting instance "NodeA3"
    And   executing workflows "Workflow Examples Project/03_Workflow Logic/03_01_Simple_Loop.wf, Workflow Examples Project/03_Workflow Logic/03_02_Forwarding_Values.wf, Workflow Examples Project/03_Workflow Logic/03_03_Nested_Loop.wf, Workflow Examples Project/03_Workflow Logic/03_04_Fault-tolerant_Loop.wf" on "NodeA3"
    And   stopping instance "NodeA3"
    
    Then   the log output of "NodeA3" should contain the pattern "(?:de.rcenvironment.core.component.api.ComponentException: Script execution error: Exception: Example failure in <script> at line number 2)"
    And    the log output of "NodeA3" should indicate a clean shutdown without checking for further warnings or errors
    And    the log output of "NodeA3" should contain 1 error

@ExampleWorkflowsFeature
@ExampleWorkflow02
@NoGUITestSuite
@BasicIntegrationTestSuite
@ExampleWorkflow0204
Scenario: Execute example workflows of the group 04_Tool Integration

    Given instance "NodeA4" using the default build
    
    When  starting instance "NodeA4"
    And   adding tool "common/Example" to "NodeA4"
    And   executing workflows "Workflow Examples Project/04_Tool Integration/04_01_Example_Integration.wf, Workflow Examples Project/04_Tool Integration/04_02_Script_And_Tool_Integration_API.wf" on "NodeA4"
    And   stopping instance "NodeA4"
    
    Then  the log output of "NodeA4" should indicate a clean shutdown with no warnings or errors

