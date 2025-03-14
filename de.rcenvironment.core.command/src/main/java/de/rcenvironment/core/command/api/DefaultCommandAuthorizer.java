package de.rcenvironment.core.command.api;

import java.util.Set;

import de.rcenvironment.core.command.spi.AbstractCommandDescription;
import de.rcenvironment.core.command.spi.MainCommandDescription;


/**
 * A default command authorizer implementation. It does not implement any authorization logic, 
 * i.e. every command is considered to be authorized.
 * 
 * @author David Wichter
 */
public class DefaultCommandAuthorizer implements ICommandAuthorizer {

    @Override
    public boolean canExecuteCommand(AbstractCommandDescription command) {
        return true;
    }

    @Override
    public Set<MainCommandDescription> getAuthorizedCommands() {
        // TODO: Implement. 
        return null;
    }

}
