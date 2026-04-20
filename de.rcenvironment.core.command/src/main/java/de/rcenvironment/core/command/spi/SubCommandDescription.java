/*
 * Copyright 2006-2026 DLR, Germany
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
 * @author David Wichter
 * @author Robert Mischke
 */
public class SubCommandDescription extends AbstractCommandDescription {

    private String qualifiedCommand;

    public SubCommandDescription(String command, String description, SingleCommandHandler handler,
        CommandModifierInfo modifiers) {
        super(command, description, handler, modifiers);
    }

    public SubCommandDescription(String command, String description, SingleCommandHandler handler,
        CommandModifierInfo modifiers, boolean isDevCommand) {
        super(command, description, handler, modifiers, isDevCommand);
    }

    public SubCommandDescription(String command, String description, SingleCommandHandler handler) {
        super(command, description, handler);
    }

    public SubCommandDescription(String command, String description, SingleCommandHandler handler,
        boolean isDevcommand) {
        super(command, description, handler, isDevcommand);
    }

    @Override
    public String getQualifiedCommand() {
        if (qualifiedCommand == null) {
            throw new IllegalStateException("Requested a qualified command, but no command group was set for " + getCommand());
        }
        return qualifiedCommand;
    }

    public void setCommandGroup(String commandGroup) {
        // technically not thread safe, but should be fine under current usage
        this.qualifiedCommand = String.format("%s %s", commandGroup, super.getCommand());
    }
}
