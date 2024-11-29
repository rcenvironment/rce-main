Feature: WorkflowTests

@WorkflowTestsFeature
@Workflow01
@NoGUITestSuite
Scenario: Execute a distributed workflow on a single node and expect fallback to local component execution

  Given running instance "NodeA" using the default build
  When  executing workflow "bdd_01_simple_distributed" on "NodeA"
  Then  the workflow should have reached the FINISHED state
  And   the workflow controller should have been "NodeA"
  And   workflow component "Joiner 1" should have been run on "NodeA"
  And   workflow component "Joiner 2" should have been run on "NodeA"
  And   workflow component "Joiner 3" should have been run on "NodeA"


@WorkflowTestsFeature
@Workflow02
@DefaultTestSuite
@NoGUITestSuite
Scenario: Execute a distributed workflow, testing the local fallback case first, and then proper remote component execution

  Given instances "NodeA, NodeB [Id=00000000000000000000000000000002]" using the default build
  And   configured network connections "NodeA->NodeB [autoStart]"
  When  starting all instances concurrently
  
  # set local access to the Joiner component on NodeB (to make sure no permissions from previous tests exist) 
  When  executing command "components set-auth rce/Joiner local" on "NodeB"
  Then  the output should contain "Set access authorization"

  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds

  # all Joiner components should fall back to NodeA as there is no such component available on NodeB
  When  executing workflow "bdd_01_simple_distributed" on "NodeA"
  Then  the workflow should have reached the FINISHED state
  And   the workflow controller should have been "NodeA"
  And   workflow component "Joiner 1" should have been run on "NodeA"
  And   workflow component "Joiner 2" should have been run on "NodeA"
  And   workflow component "Joiner 3" should have been run on "NodeA"

  # set public access to the Joiner component on NodeB 
  When  executing command "components set-auth rce/Joiner public" on "NodeB"
  Then  the output should contain "Set access authorization"

  # wait to make sure component authorization has propagated to NodeA before starting the workflow
  When  waiting for 1 second

  # now the workflow should execute Joiner 1 and 3 on NodeB as stored in the workflow file; 
  # this also verifies that the previous workflow run did not erase the node settings from the .wf file during fallback 
  When  executing workflow "bdd_01_simple_distributed" on "NodeA"
  Then  the workflow should have reached the FINISHED state
  And   the workflow controller should have been "NodeA"
  And   workflow component "Joiner 1" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Joiner 2" should have been run on "NodeA"
  And   workflow component "Joiner 3" should have been run on "NodeB" using node id "00000000000000000000000000000002"


@WorkflowTestsFeature
@Workflow03
@WfRobustness01
@NoGUITestSuite
Scenario: Network disruptions during distributed workflow with local controller

  Given instances "NodeA, NodeB [Id=00000000000000000000000000000002]" using the default build
  And   configured network connections "NodeA->NodeB [autoStart autoRetryInitialDelay=1 autoRetryDelayMultiplier=1]"
  When  starting all instances concurrently
  # set public access to the Joiner component on NodeB 
  And   executing command "components set-auth rce/Joiner public" on "NodeB"
  Then  the output should contain "Set access authorization"
  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds

  # NodeA is the instance that initiated the connection, so it must be the one to cycle the connection
  When  scheduling a reconnect of "NodeA" after 5 seconds
  And   scheduling a reconnect of "NodeA" after 10 seconds
  And   scheduling a reconnect of "NodeA" after 15 seconds
  And   scheduling a reconnect of "NodeA" after 20 seconds

  And   executing workflow "bdd_wf_robustness_basic_loop_2_nodes_wf_ctrl_undefined.wf" on "NodeA"
  Then  the workflow controller should have been "NodeA"
  And   workflow component "Local 1" should have been run on "NodeA"
  And   workflow component "Remote 2" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Local 3" should have been run on "NodeA"
  And   workflow component "Remote 4" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   the workflow should have reached the FINISHED state


@WorkflowTestsFeature
@Workflow04
@WfRobustness02
@NoGUITestSuite
Scenario: Network disruptions during distributed workflow with remote controller

  Given instances "NodeA, NodeB [WfHost;Id=00000000000000000000000000000002]" using the default build
  And   configured network connections "NodeA->NodeB [autoStart autoRetryInitialDelay=1 autoRetryDelayMultiplier=1]"
  When  starting all instances concurrently
  # set public access to the Joiner component on NodeB 
  When  executing command "components set-auth rce/Joiner public" on "NodeB"
  Then  the output should contain "Set access authorization"
  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds

  # NodeA is the instance that initiated the connection, so it must be the one to cycle the connection
  When  scheduling a reconnect of "NodeA" after 5 seconds
  And   scheduling a reconnect of "NodeA" after 10 seconds
  And   scheduling a reconnect of "NodeA" after 15 seconds
  And   scheduling a reconnect of "NodeA" after 20 seconds

  # Initiate the workflow on NodeA, but the controller is set to id 2 (NodeB) inside the workflow file
  And   executing workflow "bdd_wf_robustness_basic_loop_2_nodes_wf_ctrl_on_node2.wf" on "NodeA"
  # Ensure that the log files a written completely
  And   stopping all instances
  
  # Check debug.log files as a workaround as long as remote workflow log capture is not robust against network disruption (issue #0016137)
  Then the log output of "NodeA" should contain the pattern "Created workflow from file .* on node \"NodeB\""
  And  the log output of "NodeA" should contain the pattern "Component 'Local 1' .* is now PROCESSING_INPUTS"
  And  the log output of "NodeA" should contain the pattern "Component 'Local 3' .* is now PROCESSING_INPUTS"
  And  the log output of "NodeB" should contain the pattern "Workflow .* is now FINISHED"
  And  the log output of "NodeB" should contain the pattern "Component 'Remote 2' .* is now PROCESSING_INPUTS"
  And  the log output of "NodeB" should contain the pattern "Component 'Remote 4' .* is now PROCESSING_INPUTS"

  
@WorkflowTestsFeature
@Workflow05
@WfRobustness03
@NoGUITestSuite
Scenario: Remote node start during distributed workflow with local controller

  Given instances "NodeA, NodeB [Id=00000000000000000000000000000002]" using the default build
  And   configured network connections "NodeA->NodeB [autoStart]"
  When  starting all instances concurrently
  # set public access to the Joiner component on NodeB 
  When  executing command "components set-auth rce/Joiner public" on "NodeB"
  Then  the output should contain "Set access authorization"
  # wait for network connections after setting the component permissions to avoid needing extra wait time
  And   all auto-start network connections should be ready within 20 seconds
  
  # schedule restart of NodeB while the workflow should be running
  When  scheduling a restart of "NodeB" after 2 seconds
  And   starting workflow "bdd_wf_robustness_basic_loop_2_nodes_wf_ctrl_undefined.wf" on "NodeA"

  And   waiting for 25 seconds
  Then  the workflow controller should have been "NodeA"
  And   workflow component "Local 1" should have been run on "NodeA"
  And   workflow component "Remote 2" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  And   workflow component "Local 3" should have been run on "NodeA"
  And   workflow component "Remote 4" should have been run on "NodeB" using node id "00000000000000000000000000000002"
  # The workflow should have failed within a reasonable time (instead of waiting indefinitely)
  And   the workflow should have reached the FAILED state
  # Also, NodeA should have received and logged an exception stating that the remote node restart was specifically detected
  And   the log output of "NodeA" should contain the pattern "(?:has been restarted|the remote node was restarted|The destination instance for this request was restarted)"

  # Current workaround for the problem that NodeB may still be finishing its restart on test shutdown, causing an irrelevant failure
  And  waiting for 15 seconds
  
  
@WorkflowTestsFeature
@Workflow06
@DefaultTestSuite
@NoGUITestSuite
Scenario: Executing "wf self-test"

  Given the running instance "NodeA" using the default build

  When  executing the command "wf self-test" on "NodeA"
  Then  the output should contain "Verification SUCCEEDED"
  
  
@WorkflowTestsFeature
@Workflow07
@SSHTestSuite
@NoGUITestSuite
Scenario: Running Workflow with remote tool, published via uplink

  Given instance "Uplink1, Client1, Client2" using the default build
  And   configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry], Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
  When  starting instances "Uplink1"
  And   starting instances "Client1, Client2" concurrently 
  And   adding tool "common/TestTool" to "Client1"
  And   executing command "components set-auth common/TestTool public" on "Client1"
  And   executing workflow "UplinkRemoteWorkflow.wf" on "Client2"
    
  Then  the workflow controller should have been "Client2"
  And   workflow component "Printer" should have been run on "Client2"
  And   workflow component "TestTool" should have been run on "Client1" via uplink
    
    
@WorkflowTestsFeature
@Workflow08
@SSHTestSuite
@NoGUITestSuite
Scenario: Cancelling Workflow with remote tool, published via uplink

  Given instance "Uplink1, Client1, Client2" using the default build
  And   configured network connections "Client1-[upl]->Uplink1 [autoStart autoRetry], Client2-[upl]->Uplink1 [autoStart autoRetry]"
    
  When  starting instances "Uplink1"
  And   starting instances "Client1, Client2" concurrently 
  And   adding tool "common/LongTestTool" to "Client1"
  And   executing command "components set-auth common/LongTestTool public" on "Client1"
  And   starting workflow "UplinkRemoteWorkflowAbortLong.wf" on "Client2"
  And   waiting for 10 seconds
  And   cancelling workflow "UplinkRemoteWorkflowAbortLong.wf" on "Client2"
    
  Then  workflow component "LongTestTool" should have been cancelled
    
@WorkflowTestsFeature
@Workflow09
@NoGUITestSuite
@BasicIntegrationTestSuite
Scenario Outline: Workflow Info via Network

  Given instance "NodeA [WorkflowHost]" using the <NodeA_build> build
  And   instance "NodeB" using the <NodeB_build> build
  And   configured network connections "NodeA-[reg]->NodeB [autoStart autoRetryInitialDelay=2 autoRetryDelayMultiplier=1]"
    
  When  starting all instances concurrently
  And   adding tool "common/TestTool" to "NodeB"
  And   executing command "components set-auth common/TestTool public" on "NodeB"
  
  Then  the output should contain "Set access authorization"
  And   all auto-start network connections should be ready within 20 seconds
    
  When  executing workflow "UplinkRemoteWorkflow.wf" on "NodeA"
    
  Then  the workflow should have reached the FINISHED state
  And   instance "NodeB" should see workflow "UplinkRemoteWorkflow"
    
    Examples:
    |NodeA_build|NodeB_build|
    |default|default|
#    |default|base|
#    |base|default|
    
@WorkflowTestsFeature
@Workflow10
@NoGUITestSuite
@BasicIntegrationTestSuite
Scenario Outline: Workflow Data via Network

  Given instance "NodeA [WorkflowHost]" using the <NodeA_build> build
  And   instance "NodeB" using the <NodeB_build> build
  And   configured network connections "NodeA-[reg]->NodeB [autoStart]"
    
  When  starting all instances concurrently
  And   adding tool "common/TestTool" to "NodeB"
  And   executing command "components set-auth common/TestTool public" on "NodeB"
  
  Then  the output should contain "Set access authorization"
  And   all auto-start network connections should be ready within 20 seconds
  
  When   executing workflow "UplinkRemoteWorkflow.wf" on "NodeA"
    
  Then  the workflow should have reached the FINISHED state
  And   instances "NodeA, NodeB" should see identical data for workflow "UplinkRemoteWorkflow"
    
    Examples:
    |NodeA_build|NodeB_build|
    |default|default|
# executing these cases should be possible after the next major update (RCE 11.0), since the base version then has the necessary commands to export wf runs
#    |default|base|
#    |base|default|


# TODO fix test failure @Matthias Wagner
#@WorkflowTestsFeature
@Workflow14
@NoGUITestSuite
Scenario: Properties containing umlauts are echoed correctly

  Given running instance "NodeA" using the default build
  And   adding tool "common/Echo" to "NodeA"

  When  executing workflow "EncodingTestWorkflow" on "NodeA"

  Then  the workflow should have reached the FINISHED state
  And   that workflow run should be identical to "EncodingTestWorkflow"

  # Ensure that the log files are written completely
  When  stopping all instances
  
  Then  the log output of "NodeA" should indicate a clean shutdown with no warnings or errors
