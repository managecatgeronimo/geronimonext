/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.shell.deploy;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.geronimo.cli.deployer.BaseCommandArgs;
import org.apache.geronimo.deployment.cli.CommandInstallCAR;
import org.apache.geronimo.deployment.cli.ServerConnection;

/**
 * @version $Rev$ $Date$
 */
@Command(scope = "deploy", name = "install-plugin", description = "Install plugin")
public class InstallPluginCommand extends ConnectCommand {

    @Argument(required = true, description = "Plugin file")
    String pluginFile;

    @Override
    protected Object doExecute() throws Exception {
        ServerConnection connection = connect();

        CommandInstallCAR command = new CommandInstallCAR();

        BaseCommandArgs args = new BaseCommandArgs(pluginFile.trim().split("[ ]"));

        command.execute(this, connection, args);
        return null;
    }

}
