/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.spi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for defining command groups which define the first token of a command.
 * 
 * @author Sebastian Nocke
 * @author Robert Mischke (refactoring)
 */
public class MainCommandDescription extends AbstractCommandDescription {

    private SubCommandDescription[] subCommands;

    private Map<String, SubCommandDescription> nSubCommands = new HashMap<>();

    private String commandGroupDescription;

    public MainCommandDescription(String command, String commandGroupDescription, String description,
        SingleCommandHandler handler, CommandModifierInfo modifiers,
        boolean isDevcommand, SubCommandDescription... subCommands) {
        super(command, description, handler, modifiers, isDevcommand);
        this.commandGroupDescription = commandGroupDescription;
        this.subCommands = subCommands;
        putSubCommandsInMap();
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
        return nSubCommands.containsKey(subCommand);
    }

    public SubCommandDescription getSubCommand(String subCommand) {
        return nSubCommands.get(subCommand);
    }

    private void putSubCommandsInMap() {
        Arrays.stream(subCommands).forEach(subCommnad -> nSubCommands.put(subCommnad.getCommand(), subCommnad));
    }

    @Override
    public String getFullyQualifiedCommand() {
        return getCommand();
    }
}
