Feature: UplinkTests

# General remark:
# Many scenarios are here allowed to have warnings which indicate problems with shutdowns etc., like:
#    Error in asynchronous callback; shutting down queue
#    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
#    Failed to register channelClosed() event
#    as it is already in use by session 
# This, however, is not the ideal case, these warnings are foremost allowed because they do not affect 
# the intended test objectives, but hint to general problems at synchronizing shutdowns etc.; these 
# problems are at least partially addressed in already existing Issues.

@UplinkTestsFeature
@UplinkReviewed
@UplinkNew01
@Uplink01
Scenario: Basic operation of Uplink connections between two clients and two servers (Uplink01)

    Given instances "Server1, Server2, Client1, Client2" using the default build
    And  configured network connections "Client1-[upl]->Server1 [autoStart autoRetry]"
    And  configured network connections "Client1-[upl]->Server2"
    And  configured network connections "Client2-[upl]->Server2 [autoStart autoRetry]"
    
    When starting instances "Server1, Server2"
    And  starting instances "Client1, Client2"
    
    # verify the state of all possible connections; note that explicitly testing "absent"/"present"
    # is not needed in other test scenarios if "connected"/"disconnected" is tested anyway

	# autostart
    Then the Uplink connection from "Client1" to "Server1" should be present within 1 seconds
    Then the Uplink connection from "Client1" to "Server1" should be connected within 5 seconds
    # no autostart -> remain disconnected
    Then the Uplink connection from "Client1" to "Server2" should be present within 1 seconds
    And  the Uplink connection from "Client1" to "Server2" should be disconnected within 1 seconds
    # no connection configured
    And  the Uplink connection from "Client2" to "Server1" should be absent within 1 seconds
	# autostart
    Then the Uplink connection from "Client2" to "Server2" should be present within 1 seconds
    And  the Uplink connection from "Client2" to "Server2" should be connected within 5 seconds

    When stopping instances "Client1, Client2"
    And  stopping instances "Server1, Server2"
    
    Then the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    # tracked as #17659; remove filter when fixed    
    And  the log output of instances "Server1, Server2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    Failed to register channelClosed() event
    """


#@UplinkNew02
# TODO add an explicit test case that verifies a client making unsuccessful auto-retry attempts after a shutdown 
# or crash of the server; this check has been removed from other test cases as it is unrelated and slows them down   


@UplinkTestsFeature
@UplinkReviewed
@UplinkNew03
# TODO remove extra tags after transition
@Uplink02
@Uplink03
@Uplink04
@Uplink05
Scenario: Remote visibility of Uplink tool after graceful restarts of clients and server (UplinkNew03)

    Given instance "Server1, Client1, Client2" using the default build
    And  configured network connections "Client1-[upl]->Server1 [autoStart autoRetry]"
    And  configured network connections "Client2-[upl]->Server1 [autoStart autoRetry]"
    
    When starting instance "Server1"
    And  starting instances "Client1, Client2"
    Then the Uplink connection from "Client1" to "Server1" should be connected within 5 seconds
    And  the Uplink connection from "Client2" to "Server1" should be connected within 5 seconds

    When adding tool "common/TestTool" to "Client1"
    And  executing command "components set-auth common/TestTool public" on "Client1"
    Then instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    # restart Client1 (previous scenario "Uplink02")

    When stopping instance "Client1"
    # note: redundant check until the start/stop commands are reworked
    Then instance "Client1" should be stopped
    And  the log output of instance "Client1" should indicate a clean shutdown with no warnings or errors

	# verify that the tool has "disappeared" for Client2    
    Then instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | (absent) |

    When starting instance "Client1"
    Then the Uplink connection from "Client1" to "Server1" should be connected within 15 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    # restart Client2 (previous scenario "Uplink03")

    When stopping instance "Client2"
    # note: redundant check until the start/stop commands are reworked
    Then instance "Client2" should be stopped
    And  the log output of instance "Client2" should indicate a clean shutdown with no warnings or errors

    When starting instance "Client2"
    Then the Uplink connection from "Client2" to "Server1" should be connected within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    # restart Server1 (previous scenario "Uplink04")
    
    When stopping instance "Server1"
    # note: redundant check until the start/stop commands are reworked
    Then instance "Server1" should be stopped

    Then the Uplink connection from "Client1" to "Server1" should be disconnected within 5 seconds
    And  the Uplink connection from "Client2" to "Server1" should be disconnected within 5 seconds

    # note that this test might be unreliable as log output is buffered; generally, it should have been flushed to disk already though
    And  the log output of instance "Client1" should contain "An Uplink connection (Server1_Client1_default) finished with a warning or error"
    And  the log output of instance "Client2" should contain "An Uplink connection (Server1_Client2_default) finished with a warning or error"

    When starting instance "Server1"

    # there is no exponential back-off implemented for Uplink yet, so a connection attempt should happen every 10 seconds
    Then the Uplink connection from "Client1" to "Server1" should be connected within 12 seconds
    And  the Uplink connection from "Client2" to "Server1" should be connected within 12 seconds

    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

	# start of teardown 

    When stopping instances "Client1, Client2"
    And  stopping instance "Server1"

    # allow expected warnings caused by server restart and potential auto-retry before it was reachable again
    Then the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    An Uplink connection (Server1_Client1_default) finished with a warning or error
    An Uplink connection (Server1_Client2_default) finished with a warning or error
    java.net.ConnectException: Connection refused
    """

    # tracked as #17659; remove filter when fixed
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    Failed to register channelClosed() event
    """


@UplinkTestsFeature
@UplinkReviewed
@UplinkNew04
# TODO remove extra tags after transition
@Uplink06
@Uplink07
Scenario: Remote visibility of Uplink tool after crashes and restarts of clients and server (UplinkNew04)

    Given instance "Server1, Client1, Client2" using the default build
    And  configured network connections "Client1-[upl]->Server1 [autoStart autoRetry]"
    And  configured network connections "Client2-[upl]->Server1 [autoStart autoRetry]"
    
    When starting instance "Server1"
    And  starting instances "Client1, Client2"
    Then the Uplink connection from "Client1" to "Server1" should be connected within 5 seconds
    And  the Uplink connection from "Client2" to "Server1" should be connected within 5 seconds

    When adding tool "common/TestTool" to "Client1"
    And  executing command "components set-auth common/TestTool public" on "Client1"
    Then instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    # simulate a crash of Client1 (previous scenario "Uplink06")

    When triggering a crash of instance "Client1" and it terminated within 10 seconds
    
	# verify that the tool "disappears" for Client2 after the server recognizes "Client1" as gone
    Then instance "Client2" should see these components within 30 seconds:
        | Client1 (via Client1/default) | common/TestTool | (absent) |
        
    When starting instance "Client1"
    # note: redundant check until the start/stop commands are reworked
    Then instance "Client1" should be running

    # the server may need some time to recognize the crashed client and allow reconnection
    Then the Uplink connection from "Client1" to "Server1" should be connected within 30 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    # simulate a crash of Client2 (no previous equivalent)

    When triggering a crash of instance "Client2" and it terminated within 10 seconds

    When starting instance "Client2"
    # the server may need some time to recognize the crashed client and allow reconnection
    Then the Uplink connection from "Client2" to "Server1" should be connected within 30 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |
        
    # simulate a crash of Server1 (previous scenario "Uplink07")

    When triggering a crash of instance "Server1" and it terminated within 10 seconds
    Then the Uplink connection from "Client1" to "Server1" should be disconnected within 30 seconds
    And  the Uplink connection from "Client2" to "Server1" should be disconnected within 30 seconds
    
    # note that this test might be unreliable as log output is buffered; generally, it should have been flushed to disk already though
    And  the log output of instance "Client1" should contain "An Uplink connection (Server1_Client1_default) finished with a warning or error"
    And  the log output of instance "Client2" should contain "An Uplink connection (Server1_Client2_default) finished with a warning or error"

    When starting instance "Server1"
    
    # there is no exponential back-off implemented for Uplink yet, so a connection attempt should happen every 10 seconds
    Then the Uplink connection from "Client1" to "Server1" should be connected within 12 seconds
    And  the Uplink connection from "Client2" to "Server1" should be connected within 12 seconds
    
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

	# start of teardown 

    When stopping instances "Client1, Client2"
    When stopping instance "Server1"
    
    # allow expected warnings caused by server restart and potential auto-retry before it was reachable again
    Then the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    An Uplink connection (Server1_Client1_default) finished with a warning or error
    An Uplink connection (Server1_Client2_default) finished with a warning or error
    java.net.ConnectException: Connection refused
    """
    
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    Failed to register channelClosed() event
    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
    """


@UplinkTestsFeature
@Uplink08
Scenario: Autoconnect after startup with uplink server started before clients
    Given instance "Server1, Client1, Client2" using the default build
    And  configured network connections "Client1-[upl]->Server1 [autoStart autoRetry]"
    And  configured network connections "Client2-[upl]->Server1 [autoStart autoRetry]"

    When starting instance "Server1"
    And  starting instances "Client1, Client2"

    And  adding tool "common/TestTool" to "Client1"
    And  executing command "components set-auth common/TestTool public" on "Client1"
    #And waiting for 5 seconds
    
    Then the visible uplink network of "Client1" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should contain "Server1" within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    # Client1 is shut down first, otherwise we sometimes get an error like this:
    # ERROR - de.rcenvironment.toolkit.modules.concurrency.internal.AsyncOrderedCallbackManagerImpl$InternalAsyncOrderedCallbackQueue - Error in asynchronous callback; shutting down queue (as defined by exception policy);
    # java.lang.IllegalStateException: Service not available: de.rcenvironment.core.component.management.api.LocalComponentRegistrationService ...
    And  stopping instance "Client1"
    And  stopping instance "Client2"
    And  stopping instance "Server1"
    And  the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    session already closed
    """

@UplinkTestsFeature
@Uplink09
Scenario: Autoconnect after starting Uplink clients before the Uplink server

    Given instance "Server, Client1, Client2" using the default build
    And  configured network connections "Client1-[upl]->Server [autoStart autoRetry]"
    And  configured network connections "Client2-[upl]->Server [autoStart autoRetry]"

    # note: this step implicitly waits for both clients to complete basic startup
    When starting instances "Client1, Client2"
    And  starting instance "Server"

    And  adding tool "common/TestTool" to "Client1"
    And  executing command "components set-auth common/TestTool public" on "Client1"
    
    Then the visible uplink network of "Client1" should contain "Server" within 5 seconds
    And  the visible uplink network of "Client2" should contain "Server" within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    And  stopping instance "Client1"
    And  stopping instance "Client2"
    And  stopping instance "Server"
    And  the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    java.net.ConnectException: Connection refused
    """
    And  the log output of instance "Server" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    session already closed
    """

@UplinkTestsFeature
@Server10
Scenario: Check of disconnect and connect of clients in an uplink connection

    Given instance "Server1, Client1, Client2" using the default build
    And  configured network connections "Client1-[upl]->Server1 [autoStart autoRetry]"
    And  configured network connections "Client2-[upl]->Server1 [autoStart autoRetry]"
    
    And  starting instance "Server1"
    And  starting instances "Client1, Client2"

    And  the visible uplink network of "Client1" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 5 seconds

    And  adding tool "common/TestTool" to "Client1"
    And  executing command "components set-auth common/TestTool public" on "Client1"
    #And waiting for 15 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    When executing command "uplink stop Server1_userName" on "Client1"
    #And waiting for 15 seconds
    And  the visible uplink network of "Client1" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client1" should not be connected to "Server1" within 5 seconds
    And  executing command "uplink start Server1_userName" on "Client1"
    #And waiting for 15 seconds

    Then the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |

    When executing command "uplink stop Server1_userName" on "Client2"
    #And waiting for 15 seconds
    And  the visible uplink network of "Client2" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should not be connected to "Server1" within 5 seconds
    And  executing command "uplink start Server1_userName" on "Client2"
    #And waiting for 15 seconds

    Then the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via Client1/default) | common/TestTool | local |
    # TODO: The Server1 instance has some warnings, has to be investigated with MANTIS #17659
    #And the log output of all inServer1ces should not contain any warning

    And  stopping instance "Client1"
    And  stopping instance "Client2"
    And  stopping instance "Server1"
    And  the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors:
    """
    #Error in asynchronous callback; shutting down queue
    """
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    session already closed
    """

# TODO fix test failure @Matthias Wagner
#@UplinkTestsFeature
@Server11
Scenario: Two clients with same ID access the uplink server after startup and dis- and reconnecting, uplink server rejects the second. 

    Given instances "Server1, Client3a, Client3b" using the default build
    And  configured cloned network connections "Client3a-[upl]->Server1 [autoStart autoRetry], Client3b-[upl]->Server1 [autoStart autoRetry]"


    And  starting instance "Server1"
    And  starting instance "Client3a"

    And  the visible uplink network of "Client3a" should be connected to "Server1" within 5 seconds
    And  starting instance "Client3b"
    And  the visible uplink network of "Client3b" should not be connected to "Server1" within 5 seconds
    # The client ID "Client3a" is the cloned one, equal for all instances:
    And  the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And  the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    When executing command "uplink stop Server1_userName" on "Client3a"
    #And waiting for 15 seconds
    And  executing command "uplink start Server1_userName" on "Client3b"
    #And waiting for 15 seconds
    And  executing command "uplink start Server1_userName" on "Client3a"
    #And waiting for 15 seconds

    Then the visible uplink network of "Client3a" should not be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client3b" should be connected to "Server1" within 5 seconds
    And  the log output of "Client3a" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And  the log output of "Client3a" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."
    
    And  stopping instances "Client3a, Client3b"
    And  stopping instance "Server1"
    And  the log output of instances "Client3a, Client3b" should indicate a clean shutdown with these allowed warnings or errors:
    """
    And  client ID "Client3a" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    session already closed
    from using namespace userNameClient3a as it is already in use
    """

# TODO fix test failure @Matthias Wagner
#@UplinkTestsFeature
@Server12
Scenario: Two clients with same ID access the uplink server after startup and repeated stop/restart, uplink server rejects the second. 

    Given instances "Server1, Client3a, Client3b" using the default build
    And  configured cloned network connections "Client3a-[upl]->Server1 [autoStart autoRetry], Client3b-[upl]->Server1 [autoStart autoRetry]"

    And  starting instances "Server1"
    And  starting instance "Client3a"

    And  the visible uplink network of "Client3a" should be connected to "Server1" within 5 seconds
    And  starting instance "Client3b"

    And  the visible uplink network of "Client3b" should not be connected to "Server1" within 5 seconds
    # The client ID "Client3a" is the cloned one, equal for all instances:
    And  the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And  the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    When stopping instances "Client3a, Client3b"
    And  starting instance "Client3b"
    And  starting instance "Client3a"

    And  the visible uplink network of "Client3b" should be connected to "Server1" within 5 seconds
    Then the visible uplink network of "Client3a" should not be connected to "Server1" within 5 seconds
    And  the log output of "Client3a" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And  the log output of "Client3a" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."
    
    When stopping instances "Client3a, Client3b"
    And  starting instance "Client3a"
    And  starting instance "Client3b"

    Then the visible uplink network of "Client3a" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client3b" should not be connected to "Server1" within 5 seconds
    And  the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And  the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    And  stopping instances "Client3a, Client3b"
    And  stopping instance "Server1"
    And  the log output of instances "Client3a, Client3b" should indicate a clean shutdown with these allowed warnings or errors:
    """
    And  client ID "Client3a" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    session already closed
    from using namespace userNameClient3a as it is already in use
    """

# TODO fix test failure @Matthias Wagner
#@UplinkTestsFeature
@Server13
Scenario: Two clients with same ID access the uplink server after startup and repeated crash/restart, uplink server rejects the second. 

    Given instances "Server1, Client3a, Client3b" using the default build
    And  configured cloned network connections "Client3a-[upl]->Server1 [autoStart autoRetry], Client3b-[upl]->Server1 [autoStart autoRetry]"

    And  starting instance "Server1"
    And  starting instance "Client3a"

    And  the visible uplink network of "Client3a" should be connected to "Server1" within 5 seconds
    And  starting instance "Client3b"

    And  the visible uplink network of "Client3b" should not be connected to "Server1" within 5 seconds
    # The client ID "Client3a" is the cloned one, equal for all instances:
    And  the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And  the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    When instance "Client3a" crashes
    #And waiting for 15 seconds
    And  executing command "uplink start Server1_userName" on "Client3b"
    # We need sufficient time: after crash some clean-up work is being performed before Client3b can connect
    And  waiting for 60 seconds
    And  starting instance "Client3a"

    Then the visible uplink network of "Client3a" should not be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client3b" should be connected to "Server1" within 5 seconds
    And  the log output of "Client3a" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And  the log output of "Client3a" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."
    
    When instance "Client3b" crashes
    #And waiting for 15 seconds
    And  executing command "uplink start Server1_userName" on "Client3a"
    # We need sufficient time: after crash some clean-up work is being performed before Client3a can connect
    And  waiting for 60 seconds
    And  starting instance "Client3b"

    Then the visible uplink network of "Client3a" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client3b" should not be connected to "Server1" within 5 seconds
    And  the log output of "Client3b" should contain "Uplink handshake failed or connection refused: The combination of account name"
    And  the log output of "Client3b" should contain "and client ID \"Client3a\" is already in use. To allow parallel logins, use a different client ID for each client."

    And  stopping instances "Client3a, Client3b"
    And  stopping instance "Server1"
    And  the log output of instances "Client3a, Client3b" should indicate a clean shutdown with these allowed warnings or errors:
    """
    And  client ID "Client3a" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    session already closed
    from using namespace userNameClient3a as it is already in use
    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
    """

@UplinkTestsFeature
@Server14
Scenario: After crash, the same client connects again to the uplink server (s. Mantis #17415).

    Given instances "Server1, Client1" using the default build
    And  configured network connection "Client1-[upl]->Server1 [autoStart autoRetry]"

    And  starting instance "Server1"
    And  starting instance "Client1"

    And  the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds

    When instance "Client1" crashes
    #And waiting for 15 seconds
    And  starting instance "Client1"
    # it is essential here to give enough time to wait for the automatic reconnect
    And  waiting for 45 seconds

    Then the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds

    And  stopping instance "Client1"
    And  stopping instance "Server1"
    And  the log output of instances "Client1" should indicate a clean shutdown with these allowed warnings or errors:
    """
    And  client ID "Client1_" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    session already closed
    from using namespace userNameClient1_ as it is already in use
    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
    )
    """
# The following scenario is meant as demonstrative example to allow certain warnings at shutdown.
@UplinkExperimental01
Scenario: Check of allowed warnings after shutdown

    Given instance "Server1, Client1" using the default build
    And  configured network connections "Client1-[upl]->Server1 [autoStart autoRetry]"
    
    When starting instance "Server1"
    And  waiting for 15 seconds
    And  starting instance "Client1"
    And  waiting for 15 seconds
    And  the visible uplink network of "Client1" should be connected to "Server1"
    And  waiting for 15 seconds

    And  stopping instance "Client1"
    And  waiting for 15 seconds
    And  stopping instance "Server1"
    
    Then the log output of instance "Client1" should indicate a clean shutdown with no warnings or errors
    #Then the log output of instance "Client1" should indicate a clean shutdown with these allowed warnings or errors:
    #    """
    #    finished with a warning or error
    #    java.net.ConnectException: Connection refused
    #    """
    
    #And the log output of all instances should indicate a clean shutdown with no warnings or errors
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
        """
        stream is already closed
        as it exceeds the significant character limit (8)
        Unregistered session or session already closed
        Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
        """
    And  the log output of instance "Server1" should contain 2 warnings

