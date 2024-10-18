Feature: UplinkTests

# General remark:
# Many scenarios are here allowed to have warnings which indicate problems with shutdowns etc., like:
#    stream is already closed
#    session already closed
#    Error in asynchronous callback; shutting down queue
#    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
#    as it is already in use by session 
# This, however, is not the ideal case, these warnings are foremost allowed because they do not affect 
# the intended test objectives, but hint to general problems at synchronizing shutdowns etc.; these 
# problems are at least partially addressed in already existing Issues.

@UplinkTestsFeature
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
    
	# TODO non-semantic warnings; remove once possible
    Then the log output of instances "Client1, Client2" should indicate a clean shutdown with no warnings or errors
    And  the log output of instances "Server1, Server2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    """

@UplinkTestsFeature
@Uplink02
Scenario: Remote visibility of Uplink tool after shutdown and restart of providing client (Uplink02)

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
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Client1"
    # note: redundant check until the start/stop commands are reworked
    Then instance "Client1" should be stopped

	# verify that the tool has "disappeared" for Client2    
    Then instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | (absent) |

    When starting instance "Client1"
    Then the Uplink connection from "Client1" to "Server1" should be connected within 15 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instances "Client1, Client2"
    And  stopping instance "Server1"

	# TODO non-semantic warnings; remove once possible
    Then the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors:
    """
    #Error in asynchronous callback; shutting down queue
    """
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    """

# Variant of @Uplink02: crash instead of restart
@UplinkTestsFeature
# TODO rename this to Uplink02b instead?
@Uplink06
Scenario: Remote visibility of Uplink tool after crash and restart of providing client (Uplink06)

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
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When instance "Client1" crashes
    
	# verify that the tool "disappears" for Client2 after some time
    Then instance "Client2" should see these components within 15 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | (absent) |
        
    When starting instance "Client1"
    Then the Uplink connection from "Client1" to "Server1" should be connected within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instances "Client1, Client2"
    When stopping instance "Server1"
    
	# TODO non-semantic warnings; remove once possible
    Then the log output of instances "Client1" should indicate a clean shutdown with these allowed warnings or errors:
    """
    And  client ID "Client1_" is already in use. To allow parallel logins, use a different client ID for each client.
    finished with a warning or error; inspect the log output above for details
    """
    And  the log output of instances "Client2" should indicate a clean shutdown with these allowed warnings or errors:
    """
    #Error in asynchronous callback; shutting down queue
    """
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    Session terminated in non-terminal state UNCLEAN_SHUTDOWN_INITIATED
    as it is already in use by session
    """


@UplinkTestsFeature
@Uplink03
Scenario: Remote visibility of Uplink tool after shutdown and restart of receiving client (Uplink03)
# TODO add Client2 crash test case too? 

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
        | Client1 (via userName/Client1_) | common/TestTool | local |
        
    When stopping instance "Client2"
    # note: redundant check until the start/stop commands are reworked
    Then instance "Client2" should be stopped

    When starting instance "Client2"
    Then the Uplink connection from "Client2" to "Server1" should be connected within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instances "Client1, Client2"
    And  stopping instance "Server1"
    
	# TODO non-semantic warnings; remove once possible
    Then the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors:
    """
    #Error in asynchronous callback; shutting down queue
    """
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    """

@UplinkTestsFeature
@Uplink04
Scenario: Uplink clients auto-reconnecting after shutdown and restart of server (Uplink04)

    Given instance "Server1, Client1, Client2" using the default build
    And  configured network connections "Client1-[upl]->Server1 [autoStart autoRetry]"
    And  configured network connections "Client2-[upl]->Server1 [autoStart autoRetry]"
    
    And  starting instance "Server1"
    And  starting instances "Client1, Client2"

    Then the Uplink connection from "Client1" to "Server1" should be connected within 5 seconds
    And  the Uplink connection from "Client2" to "Server1" should be connected within 5 seconds

    When adding tool "common/TestTool" to "Client1"
    And  executing command "components set-auth common/TestTool public" on "Client1"
    Then instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Server1"
    # note: redundant check until the start/stop commands are reworked
    Then instance "Server1" should be stopped
    And  the Uplink connection from "Client1" to "Server1" should be disconnected within 5 seconds
    And  the Uplink connection from "Client2" to "Server1" should be disconnected within 5 seconds
    
    # the current Uplink auto-retry delay is hardcoded to 10 seconds, so make sure a (failing) attempt was made
    When waiting for 12 seconds
    
    # TODO this test could be unreliable as log output is buffered, so these log lines might not be on disk yet
    Then the log output of instance "Client1" should contain "An Uplink connection (Server1_userName) finished with a warning or error"
    And  the log output of instance "Client1" should contain "java.net.ConnectException: Connection refused"
    And  the log output of instance "Client2" should contain "An Uplink connection (Server1_userName) finished with a warning or error"
    And  the log output of instance "Client2" should contain "java.net.ConnectException: Connection refused"

    When starting instance "Server1"
    # there is no exponential back-off implemented for Uplink yet, so a connection attempt should happen every 10 seconds
    Then the Uplink connection from "Client1" to "Server1" should be connected within 12 seconds
    And  the Uplink connection from "Client2" to "Server1" should be connected within 12 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instances "Client1, Client2"
    And  stopping instance "Server1"
    
    Then the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    An Uplink connection (Server1_userName) finished with a warning or error
    java.net.ConnectException: Connection refused
    """

	# TODO non-semantic warnings; remove once possible
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    """

# Variant of @Uplink04: crash instead of restart
@UplinkTestsFeature
# TODO rename this to Uplink04b?
@Uplink07
Scenario: Uplink clients auto-reconnecting after crash and restart of server (Uplink07)


    Given instance "Server1, Client1, Client2" using the default build
    And  configured network connections "Client1-[upl]->Server1 [autoStart autoRetry]"
    And  configured network connections "Client2-[upl]->Server1 [autoStart autoRetry]"
    
    And  starting instance "Server1"
    And  starting instances "Client1, Client2"

    Then the Uplink connection from "Client1" to "Server1" should be connected within 5 seconds
    And  the Uplink connection from "Client2" to "Server1" should be connected within 5 seconds

    When adding tool "common/TestTool" to "Client1"
    And  executing command "components set-auth common/TestTool public" on "Client1"
    Then instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When instance "Server1" crashes
    Then the Uplink connection from "Client1" to "Server1" should be disconnected within 15 seconds
    And  the Uplink connection from "Client2" to "Server1" should be disconnected within 15 seconds
    
    # the current Uplink auto-retry delay is hardcoded to 10 seconds, so make sure a (failing) attempt was made
    When waiting for 12 seconds
    
    # TODO this test could be unreliable as log output is buffered, so these log lines might not be on disk yet
    Then the log output of instance "Client1" should contain "An Uplink connection (Server1_userName) finished with a warning or error"
    And  the log output of instance "Client1" should contain "java.net.ConnectException: Connection refused"
    And  the log output of instance "Client2" should contain "An Uplink connection (Server1_userName) finished with a warning or error"
    And  the log output of instance "Client2" should contain "java.net.ConnectException: Connection refused"

    When starting instance "Server1"
    # there is no exponential back-off implemented for Uplink yet, so a connection attempt should happen every 10 seconds
    Then the Uplink connection from "Client1" to "Server1" should be connected within 12 seconds
    And  the Uplink connection from "Client2" to "Server1" should be connected within 12 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instances "Client1, Client2"
    And  stopping instance "Server1"
    
    Then the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    An Uplink connection (Server1_userName) finished with a warning or error
    java.net.ConnectException: Connection refused
    """

	# TODO non-semantic warnings; remove once possible
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    """

# This combines the previous restart options. It is meant to be used in the context of automated
# regression testing in order to save time (compared with running each case independently).
#Combination of @Uplink02, @Uplink03, @Uplink4
@UplinkTestsFeature
@Uplink05
Scenario: Combined check of uplink autoconnect after shutdown and restart of clients and uplink server

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
    #And waiting for 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Client1"
    And  instance "Client1" should be stopped
    And  the visible uplink network of "Client2" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 5 seconds
    And  starting instance "Client1"

    Then the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Client2"
    And  instance "Client2" should be stopped
    And  the visible uplink network of "Client1" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds
    And  starting instance "Client2"

    Then the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When stopping instance "Server1"
    And  instances "Server1" should be stopped
    And  the visible uplink network of "Client1" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client1" should not be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should not be connected to "Server1" within 5 seconds
    And  starting instance "Server1"
    
    Then the visible uplink network of "Client1" should be connected to "Server1" within 15 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 15 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    And  stopping instances "Client1, Client2"
    And  stopping instance "Server1"
    And  the log output of instances "Client1, Client2" should indicate a clean shutdown with these allowed warnings or errors:
    """
    #Error in asynchronous callback; shutting down queue
    """
    #And waiting for 15 seconds
    And  the log output of instance "Server1" should indicate a clean shutdown with these allowed warnings or errors: 
    """
    as it exceeds the significant character limit (8)
    stream is already closed
    session already closed
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
        | Client1 (via userName/Client1_) | common/TestTool | local |

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
        | Client1 (via userName/Client1_) | common/TestTool | local |

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
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When executing command "uplink stop Server1_userName" on "Client1"
    #And waiting for 15 seconds
    And  the visible uplink network of "Client1" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client1" should not be connected to "Server1" within 5 seconds
    And  executing command "uplink start Server1_userName" on "Client1"
    #And waiting for 15 seconds

    Then the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |

    When executing command "uplink stop Server1_userName" on "Client2"
    #And waiting for 15 seconds
    And  the visible uplink network of "Client2" should contain "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should not be connected to "Server1" within 5 seconds
    And  executing command "uplink start Server1_userName" on "Client2"
    #And waiting for 15 seconds

    Then the visible uplink network of "Client1" should be connected to "Server1" within 5 seconds
    And  the visible uplink network of "Client2" should be connected to "Server1" within 5 seconds
    And  instance "Client2" should see these components within 5 seconds:
        | Client1 (via userName/Client1_) | common/TestTool | local |
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

