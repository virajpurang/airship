package com.proofpoint.galaxy.standalone;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.NullOutputStream;
import com.proofpoint.galaxy.shared.Assignment;
import com.proofpoint.galaxy.shared.UpgradeVersions;
import com.proofpoint.log.Logging;
import com.proofpoint.log.LoggingConfiguration;
import org.iq80.cli.Arguments;
import org.iq80.cli.Cli;
import org.iq80.cli.Cli.CliBuilder;
import org.iq80.cli.Command;
import org.iq80.cli.Help;
import org.iq80.cli.Option;
import org.iq80.cli.ParseException;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.proofpoint.galaxy.shared.SlotLifecycleState.RESTARTING;
import static com.proofpoint.galaxy.shared.SlotLifecycleState.RUNNING;
import static com.proofpoint.galaxy.shared.SlotLifecycleState.STOPPED;
import static com.proofpoint.galaxy.standalone.Column.binary;
import static com.proofpoint.galaxy.standalone.Column.config;
import static com.proofpoint.galaxy.standalone.Column.instanceType;
import static com.proofpoint.galaxy.standalone.Column.ip;
import static com.proofpoint.galaxy.standalone.Column.location;
import static com.proofpoint.galaxy.standalone.Column.shortId;
import static com.proofpoint.galaxy.standalone.Column.status;
import static com.proofpoint.galaxy.standalone.Column.statusMessage;
import static org.iq80.cli.Cli.buildCli;

public class Galaxy
{
    private static final File CONFIG_FILE = new File(System.getProperty("user.home", "."), ".galaxyconfig");

    public static void main(String[] args)
            throws Exception
    {
        CliBuilder<Callable> builder = buildCli("galaxy", Callable.class)
                .withDescription("cloud management system")
                .withDefaultCommand(HelpCommand.class)
                .withCommands(HelpCommand.class,
                        ShowCommand.class,
                        InstallCommand.class,
                        UpgradeCommand.class,
                        TerminateCommand.class,
                        StartCommand.class,
                        StopCommand.class,
                        RestartCommand.class,
                        SshCommand.class,
                        ResetToActualCommand.class);

        builder.withGroup("agent")
                .withDescription("Manage agents")
                .withDefaultCommand(AgentShowCommand.class)
                .withCommands(AgentShowCommand.class,
                        AgentAddCommand.class,
                        AgentTerminateCommand.class);

        builder.withGroup("environment")
                .withDescription("Manage environments")
                .withDefaultCommand(Help.class)
                .withCommands(EnvironmentProvisionLocal.class,
                        EnvironmentAdd.class);

        builder.withGroup("config")
                .withDescription("Manage configuration")
                .withDefaultCommand(Help.class)
                .withCommands(ConfigGet.class,
                        ConfigGetAll.class,
                        ConfigSet.class,
                        ConfigAdd.class,
                        ConfigUnset.class);

        Cli<Callable> galaxyParser = builder.build();

        galaxyParser.parse(args).call();
    }

    public static abstract class GalaxyCommand implements Callable<Void>
    {
        @Inject
        public GlobalOptions globalOptions = new GlobalOptions();

        @Override
        public Void call()
                throws Exception
        {
            initializeLogging();

            Config config = Config.loadConfig(CONFIG_FILE);

            String environment = globalOptions.environment;
            if (environment == null) {
                throw new RuntimeException("You must specify an environment.");
            }
            String coordinator = config.get("environment." + environment + ".coordinator");
            if (coordinator == null) {
                throw new RuntimeException("Unknown environment " + environment);
            }

            URI coordinatorUri = new URI(coordinator);

            Commander commander = new CommanderFactory()
                    .setEnvironment(environment)
                    .setCoordinatorUri(coordinatorUri)
                    .setRepositories(config.getAll("environment." + environment + ".repository"))
                    .setMavenDefaultGroupIds(config.getAll("environment." + environment + ".maven-group-id"))
                    .build();

            try {
                execute(commander);
            }
            catch (Exception e) {
                if (globalOptions.debug) {
                    throw e;
                }
                else {
                    System.out.println(firstNonNull(e.getMessage(), "Unknown error"));
                }
            }

            return null;
        }

        private void initializeLogging()
                throws IOException
        {
            // unhook out and err while initializing logging or logger will print to them
            PrintStream out = System.out;
            PrintStream err = System.err;
            try {
                if (globalOptions.debug) {
                    Logging logging = new Logging();
                    logging.initialize(new LoggingConfiguration());
                }
                else {
                    System.setOut(new PrintStream(new NullOutputStream()));
                    System.setErr(new PrintStream(new NullOutputStream()));

                    Logging logging = new Logging();
                    logging.initialize(new LoggingConfiguration());
                    logging.disableConsole();
                }
            }
            finally {
                System.setOut(out);
                System.setErr(err);
            }
        }

        public abstract void execute(Commander commander)
                throws Exception;

        public void displaySlots(Iterable<Record> slots)
        {
            if (Iterables.isEmpty(slots)) {
                System.out.println("No slots match the provided filters.");
            }
            else {
                TablePrinter tablePrinter = new TablePrinter(shortId, ip, status, binary, config, statusMessage);
                tablePrinter.print(slots);
            }
        }

        public void displayAgents(Iterable<Record> agents)
        {
            if (Iterables.isEmpty(agents)) {
                System.out.println("No agents match the provided filters.");
            }
            else {
                TablePrinter tablePrinter = new TablePrinter(shortId, ip, status, instanceType, location);
                tablePrinter.print(agents);
            }
        }
    }

    @Command(name = "help", description = "Display help information about galaxy")
    public static class HelpCommand extends GalaxyCommand
    {
        @Inject
        public Help help;

        @Override
        public Void call()
                throws Exception
        {
            help.call();
            return null;
        }

        @Override
        public void execute(Commander commander)
                throws Exception
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("HelpCommand");
            sb.append("{help=").append(help);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "show", description = "Show state of all slots")
    public static class ShowCommand extends GalaxyCommand
    {
        @Inject
        public final SlotFilter slotFilter = new SlotFilter();

        @Override
        public void execute(Commander commander)
        {
            List<Record> slots = commander.show(slotFilter);
            displaySlots(slots);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("ShowCommand");
            sb.append("{slotFilter=").append(slotFilter);
            sb.append(", globalOptions=").append(globalOptions);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "install", description = "Install software in a new slot")
    public static class InstallCommand extends GalaxyCommand
    {
        @Option(name = {"--count"}, description = "Number of instances to install")
        public int count = 1;

        @Inject
        public final AgentFilter agentFilter = new AgentFilter();

        @Arguments(usage = "<groupId:artifactId[:packaging[:classifier]]:version> @<component:pools:version>",
                description = "The binary and @configuration to install.  The default packaging is tar.gz")
        public final List<String> assignment = Lists.newArrayList();

        @Override
        public void execute(Commander commander)
        {
            if (assignment.size() != 2) {
                throw new ParseException("You must specify a binary and config to install.");
            }
            String binary;
            String config;
            if (assignment.get(0).startsWith("@")) {
                config = assignment.get(0);
                binary = assignment.get(1);
            }
            else {
                binary = assignment.get(0);
                config = assignment.get(1);
            }

            Assignment assignment = new Assignment(binary, config);
            List<Record> slots = commander.install(agentFilter, count, assignment);
            displaySlots(slots);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("InstallCommand");
            sb.append("{count=").append(count);
            sb.append(", agentFilter=").append(agentFilter);
            sb.append(", assignment=").append(assignment);
            sb.append(", globalOptions=").append(globalOptions);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "upgrade", description = "Upgrade software in a slot")
    public static class UpgradeCommand extends GalaxyCommand
    {
        @Inject
        public final SlotFilter slotFilter = new SlotFilter();

        @Arguments(usage = "[<binary-version>] [@<config-version>]",
                description = "Version of the binary and/or @configuration")
        public final List<String> versions = Lists.newArrayList();

        @Override
        public void execute(Commander commander)
        {
            if (versions.size() != 1 && versions.size() != 2) {
                throw new ParseException("You must specify a binary version or a config version for upgrade.");
            }

            String binaryVersion = null;
            String configVersion = null;
            if (versions.get(0).startsWith("@")) {
                configVersion = versions.get(0);
                if (versions.size() > 1) {
                    binaryVersion = versions.get(1);
                }
            }
            else {
                binaryVersion = versions.get(0);
                if (versions.size() > 1) {
                    configVersion = versions.get(1);
                }
            }

            UpgradeVersions upgradeVersions = new UpgradeVersions(binaryVersion, configVersion);
            List<Record> slots = commander.upgrade(slotFilter, upgradeVersions);
            displaySlots(slots);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("UpgradeCommand");
            sb.append("{slotFilter=").append(slotFilter);
            sb.append(", versions=").append(versions);
            sb.append(", globalOptions=").append(globalOptions);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "terminate", description = "Terminate (remove) a slot")
    public static class TerminateCommand extends GalaxyCommand
    {
        @Inject
        public final SlotFilter slotFilter = new SlotFilter();

        @Override
        public void execute(Commander commander)
        {
            List<Record> slots = commander.terminate(slotFilter);
            displaySlots(slots);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("TerminateCommand");
            sb.append("{slotFilter=").append(slotFilter);
            sb.append(", globalOptions=").append(globalOptions);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "start", description = "Start a server")
    public static class StartCommand extends GalaxyCommand
    {
        @Inject
        public final SlotFilter slotFilter = new SlotFilter();

        @Override
        public void execute(Commander commander)
        {
            List<Record> slots = commander.setState(slotFilter, RUNNING);
            displaySlots(slots);
        }
    }

    @Command(name = "stop", description = "Stop a server")
    public static class StopCommand extends GalaxyCommand
    {
        @Inject
        public final SlotFilter slotFilter = new SlotFilter();

        @Override
        public void execute(Commander commander)
        {
            List<Record> slots = commander.setState(slotFilter, STOPPED);
            displaySlots(slots);
        }
    }

    @Command(name = "restart", description = "Restart server")
    public static class RestartCommand extends GalaxyCommand
    {
        @Inject
        public final SlotFilter slotFilter = new SlotFilter();

        @Override
        public void execute(Commander commander)
        {
            List<Record> slots = commander.setState(slotFilter, RESTARTING);
            displaySlots(slots);
        }
    }

    @Command(name = "reset-to-actual", description = "Reset slot expected state to actual")
    public static class ResetToActualCommand extends GalaxyCommand
    {
        @Inject
        public final SlotFilter slotFilter = new SlotFilter();

        @Override
        public void execute(Commander commander)
        {
            List<Record> slots = commander.resetExpectedState(slotFilter);
            displaySlots(slots);
        }
    }

    @Command(name = "ssh", description = "ssh to slot installation")
    public static class SshCommand extends GalaxyCommand
    {
        @Inject
        public final SlotFilter slotFilter = new SlotFilter();

        @Arguments(description = "Command to execute on the remote host")
        public String command;

        @Override
        public void execute(Commander commander)
        {
            commander.ssh(slotFilter, command);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("InstallCommand");
            sb.append("{slotFilter=").append(slotFilter);
            sb.append(", args=").append(command);
            sb.append(", globalOptions=").append(globalOptions);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "show", description = "Show agent details")
    public static class AgentShowCommand extends GalaxyCommand
    {
        @Inject
        public final AgentFilter agentFilter = new AgentFilter();

        @Override
        public void execute(Commander commander)
                throws Exception
        {
            List<Record> agents = commander.showAgents(agentFilter);
            displayAgents(agents);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("AgentShowCommand");
            sb.append("{globalOptions=").append(globalOptions);
            sb.append(", agentFilter=").append(agentFilter);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "add", description = "Provision a new agent")
    public static class AgentAddCommand extends GalaxyCommand
    {
        @Option(name = {"--count"}, description = "Number of agents to provision")
        public int count = 1;

        @Option(name = {"--availability-zone"}, description = "Availability zone to provision")
        public String availabilityZone;

        @Arguments(usage = "[<instance-type>]", description = "Instance type to provision")
        public String instanceType;

        @Override
        public void execute(Commander commander)
                throws Exception
        {
            List<Record> agents = commander.addAgents(count, availabilityZone, instanceType);
            displayAgents(agents);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("AgentAddCommand");
            sb.append("{count=").append(count);
            sb.append(", availabilityZone='").append(availabilityZone).append('\'');
            sb.append(", instanceType=").append(instanceType);
            sb.append(", globalOptions=").append(globalOptions);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "terminate", description = "Terminate an agent")
    public static class AgentTerminateCommand extends GalaxyCommand
    {
        @Arguments(title = "agent-id", description = "Agent to terminate", required = true)
        public String agentId;

        @Override
        public void execute(Commander commander)
                throws Exception
        {
            Record agent = commander.terminateAgent(agentId);
            displayAgents(ImmutableList.of(agent));
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("AgentTerminateCommand");
            sb.append("{agentId='").append(agentId).append('\'');
            sb.append(", globalOptions=").append(globalOptions);
            sb.append('}');
            return sb.toString();
        }
    }

    @Command(name = "provision-local", description = "Provision a local environment")
    public static class EnvironmentProvisionLocal implements Callable<Void>
    {
        @Option(name = "--repository", description = "Repository for binaries and configurations")
        public final List<String> repository = newArrayList();

        @Option(name = "--maven-default-group-id", description = "Default maven group-id")
        public final List<String> mavenDefaultGroupId = newArrayList();

        @Arguments(usage = "<environment> <path>",
                description = "Environment name and path for the environment")
        public List<String> args;

        @Override
        public Void call()
                throws Exception
        {
            if (args.size() != 2) {
                throw new ParseException("You must specify an environment and path.");
            }

            String environment = args.get(0);
            String path = args.get(1);

            String coordinatorProperty = "environment." + environment + ".coordinator";
            String repositoryProperty = "environment." + environment + ".repository";
            String mavenGroupIdProperty = "environment." + environment + ".maven-group-id";

            Config config = Config.loadConfig(CONFIG_FILE);
            if (config.get(coordinatorProperty) != null) {
                throw new RuntimeException("Environment " + environment + " already exists");
            }
            config.set(coordinatorProperty, path);
            for (String repo : repository) {
                config.add(repositoryProperty, repo);
            }
            for (String groupId : mavenDefaultGroupId) {
                config.add(mavenGroupIdProperty, groupId);
            }
            config.save(CONFIG_FILE);
            return null;
        }
    }

    @Command(name = "add", description = "Add an environment")
    public static class EnvironmentAdd implements Callable<Void>
    {
        @Arguments(usage = "<environment> <coordinator-url>",
                description = "Environment name and a coordinator url for the environment")
        public List<String> args;

        @Override
        public Void call()
                throws Exception
        {
            if (args.size() != 2) {
                throw new ParseException("You must specify an environment and a coordinator URL.");
            }

            String environment = args.get(0);
            String coordinatorUrl = args.get(1);

            String coordinatorProperty = "environment." + environment + ".coordinator";

            Config config = Config.loadConfig(CONFIG_FILE);
            if (config.get(coordinatorProperty) != null) {
                throw new RuntimeException("Environment " + environment + " already exists");
            }
            config.set(coordinatorProperty, coordinatorUrl);
            config.save(CONFIG_FILE);
            return null;
        }
    }

    @Command(name = "get", description = "Get a configuration value")
    public static class ConfigGet implements Callable<Void>
    {
        @Arguments(description = "Key to get")
        public String key;

        @Override
        public Void call()
                throws Exception
        {
            Preconditions.checkNotNull(key, "You must specify a key.");

            Config config = Config.loadConfig(CONFIG_FILE);
            List<String> values = config.getAll(key);
            Preconditions.checkArgument(values.size() < 2, "More than one value for the key %s", key);
            if (!values.isEmpty()) {
                System.out.println(values.get(0));
            }
            return null;
        }
    }

    @Command(name = "get-all", description = "Get all values of configuration")
    public static class ConfigGetAll implements Callable<Void>
    {
        @Arguments(description = "Key to get")
        public String key;

        @Override
        public Void call()
                throws Exception
        {
            Preconditions.checkNotNull(key, "You must specify a key.");

            Config config = Config.loadConfig(CONFIG_FILE);
            List<String> values = config.getAll(key);
            for (String value : values) {
                System.out.println(value);
            }
            return null;
        }
    }

    @Command(name = "set", description = "Set a configuration value")
    public static class ConfigSet implements Callable<Void>
    {
        @Arguments(usage = "<key> <value>",
                description = "Key-value pair to set")
        public List<String> args;

        @Override
        public Void call()
                throws Exception
        {
            if (args.size() != 2) {
                throw new ParseException("You must specify a key and a value.");
            }

            String key = args.get(0);
            String value = args.get(1);

            Config config = Config.loadConfig(CONFIG_FILE);
            config.set(key, value);
            config.save(CONFIG_FILE);
            return null;
        }
    }

    @Command(name = "add", description = "Add a configuration value")
    public static class ConfigAdd implements Callable<Void>
    {
        @Arguments(usage = "<key> <value>",
                description = "Key-value pair to add")
        public List<String> args;

        @Override
        public Void call()
                throws Exception
        {
            if (args.size() != 2) {
                throw new ParseException("You must specify a key and a value.");
            }

            String key = args.get(0);
            String value = args.get(1);

            Config config = Config.loadConfig(CONFIG_FILE);
            config.add(key, value);
            config.save(CONFIG_FILE);
            return null;
        }
    }

    @Command(name = "unset", description = "Unset a configuration value")
    public static class ConfigUnset implements Callable<Void>
    {
        @Arguments(description = "Key to unset")
        public String key;

        @Override
        public Void call()
                throws Exception
        {
            Preconditions.checkNotNull(key, "You must specify a key.");

            Config config = Config.loadConfig(CONFIG_FILE);
            config.unset(key);
            config.save(CONFIG_FILE);
            return null;
        }
    }
}