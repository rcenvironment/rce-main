package de.rcenvironment.core.command.api;

import java.util.Set;

import de.rcenvironment.core.command.spi.ExecutableCommand;


/**
 * A default command authorizer implementation. It does not implement any authorization logic, 
 * i.e. every command is considered to be authorized.
 * 
 * @author David Wichter
 */
public class DefaultCommandAuthorizer implements ICommandAuthorizer {

    @Override
    public boolean canExecuteCommand(ExecutableCommand command) {
        return true;
    }

    @Override
    public Set<ExecutableCommand> getAuthorizedCommands() {
        // TODO: Implement. 
        return null;
    }

}
