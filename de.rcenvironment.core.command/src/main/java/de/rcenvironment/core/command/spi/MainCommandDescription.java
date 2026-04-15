/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * Used for defining command groups which define the first token of a command.
 * 
 * @author Sebastian Nocke
 * @author Robert Mischke
 */
public class MainCommandDescription extends AbstractCommandDescription {

    private final String commandGroupDescription;

    private final SubCommandDescription[] subCommands;

    private final Map<String, SubCommandDescription> subCommandsById = new HashMap<>();

    public MainCommandDescription(String mainCommand, String commandGroupDescription, String description,
        SingleCommandHandler handler, CommandModifierInfo modifiers,
        boolean isDevcommand, SubCommandDescription... subCommandsParam) {
        super(mainCommand, description, handler, modifiers, isDevcommand);
        this.commandGroupDescription = commandGroupDescription;
        if (subCommandsParam != null) {
            this.subCommands = subCommandsParam;
        } else {
            this.subCommands = new SubCommandDescription[0];
        }
        for (SubCommandDescription sc : this.subCommands) {
            sc.setCommandGroup(mainCommand);
            subCommandsById.put(sc.getCommand(), sc);
        }
    }

    public MainCommandDescription(String command, String commandGroupDescription, String description,
        SingleCommandHandler handler, CommandModifierInfo modifiers, SubCommandDescription... subCommands) {
        this(command, commandGroupDescription, description, handler, modifiers, false, subCommands);
    }

    public MainCommandDescription(String command, String commandGroupDescription, String description,
        SingleCommandHandler handler, SubCommandDescription... subCommands) {
        this(command, commandGroupDescription, description, handler, new CommandModifierInfo(), false, subCommands);
    }

    public MainCommandDescription(String command, String commandGroupDescription, String description,
        SingleCommandHandler handler, boolean isDevcommand, SubCommandDescription... subCommands) {
        this(command, commandGroupDescription, description, handler, new CommandModifierInfo(), isDevcommand, subCommands);
    }

    public MainCommandDescription(String command, String commandGroupDescription,
        String description, SubCommandDescription... subCommands) {
        this(command, commandGroupDescription, description, null, new CommandModifierInfo(), false, subCommands);
    }

    public MainCommandDescription(String command, String commandGroupDescription, String description,
        boolean isDevcommand, SubCommandDescription... subCommands) {
        this(command, commandGroupDescription, description, null, new CommandModifierInfo(), isDevcommand, subCommands);
    }

    public SubCommandDescription[] getSubCommands() {
        return subCommands;
    }

    public String getCommandGroupDescription() {
        return commandGroupDescription;
    }

    public boolean hasSubCommand(String subCommand) {
        return subCommandsById.containsKey(subCommand);
    }

    public SubCommandDescription getSubCommand(String subCommand) {
        return subCommandsById.get(subCommand);
    }

    @Override
    public String getQualifiedCommand() {
        return getCommand();
    }
}
