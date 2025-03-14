/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

/**
 * Used for defining sub commands of a command group {@link MainCommandDescription}.
 * 
 * @author Sebastian Nocke
 *
 */
public class SubCommandDescription extends AbstractCommandDescription {
    
    private final String commandGroup;

    public SubCommandDescription(String commandGroup, String command, String description,
        SingleCommandHandler handler, CommandModifierInfo modifiers) {

        super(command, description, handler, modifiers);
        this.commandGroup = commandGroup;
    }
    
    public SubCommandDescription(String commandGroup, String command, String description,
            SingleCommandHandler handler, CommandModifierInfo modifiers, boolean isDevCommand) {
        super(command, description, handler, modifiers, isDevCommand);
        this.commandGroup = commandGroup;
    }
    
    public SubCommandDescription(String commandGroup, String command, String description, SingleCommandHandler handler) {
        super(command, description, handler);
        this.commandGroup = commandGroup;
    }
    
    public SubCommandDescription(String commandGroup, String command, String description, 
        SingleCommandHandler handler, boolean isDevcommand) {

        super(command, description, handler, isDevcommand);
        this.commandGroup = commandGroup;
    }

    @Override
    public String getFullyQualifiedCommand() {
        return String.format("%s %s", commandGroup, super.getCommand());
    }
}
