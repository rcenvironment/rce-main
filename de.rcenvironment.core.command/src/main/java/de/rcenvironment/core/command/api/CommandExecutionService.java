/*
 * Copyright 2006-2026 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.command.api;

import java.util.List;
import java.util.concurrent.Future;

import de.rcenvironment.core.command.spi.CommandParser;
import de.rcenvironment.core.command.spi.ParsedCommandModifiers;
import de.rcenvironment.core.utils.common.textstream.TextOutputReceiver;

/**
 * Interface for the execution of string commands. Examples of commands are workflow execution, network status and manipulation, and
 * developer/debug output generation.
 * 
 * @author Robert Mischke
 */
public interface CommandExecutionService {

    /**
     * Asynchronously executes a multi-command. A multi-command is a list of string tokens describing one or more commands, which are to be
     * executed sequentially and/or in parallel. Special tokens are used to separate individual commands, and to mark commands for parallel
     * execution. The default is sequential execution.
     * 
     * TODO add documentation of special tokens; add examples
     * 
     * All output is sent to the provided {@link TextOutputReceiver}. Before execution starts, {@link TextOutputReceiver#onStart()} is
     * called. If an error occurs during execution, it is passed to {@link TextOutputReceiver#onFatalError(Exception)}. If no exception
     * occurs, {@link TextOutputReceiver#onFinished()} is called after the last command has terminated.
     * 
     * @param tokens the tokens of the multi-command to execute
     * @param outputReceiver the receiver of output and execution lifecycle events
     * @param cmdAuthFilter A {@link CommandAuthFilter} that is used to check whether the command is currently authorized to be executed. If
     *        not, then the command is not executed.
     * @param initiator an arbitrary object providing information about the source that initiated this command; may be used to check account
     *        permissions
     * @return a {@link Future} for a {@link CommandExecutionResult} that can be waited for to detect the end of command execution
     */
    Future<CommandExecutionResult> asyncExecMultiCommand(List<String> tokens, TextOutputReceiver outputReceiver,
        CommandAuthFilter cmdAuthFilter, Object initiator);

    void printHelpText(ParsedCommandModifiers modifiers, TextOutputReceiver outputReceiver, CommandAuthFilter cmdAuthFilter);
        
    /**
     * Prints the same output as {@link #printHelpText(boolean, TextOutputReceiver)}, but collects the output and returns it as a
     * newline-separated string.
     * 
     * @param addCommonPrefix true if the common prefix ("rce ...") should be added
     * @param showDevCommands whether developer commands should be included
     * @param cmdAuthFilter A {@link CommandAuthFilter} that is used to show authorized commands only
     * @return the collected output
     */
    String getHelpText(boolean addCommonPrefix, boolean showDevCommands, CommandAuthFilter cmdAuthFilter);

    CommandParser getParser();

}
