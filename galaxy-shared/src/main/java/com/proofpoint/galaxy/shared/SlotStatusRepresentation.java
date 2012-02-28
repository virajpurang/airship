/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.proofpoint.galaxy.shared;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.validation.constraints.NotNull;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;

@JsonAutoDetect(JsonMethod.NONE)
public class SlotStatusRepresentation
{
    private final UUID id;
    private final String shortId;
    private final String name;
    private final URI self;
    private final URI externalUri;
    private final String location;
    private final String binary;
    private final String shortBinary;
    private final String config;
    private final String shortConfig;
    private final String status;
    private final String version;
    private final String statusMessage;
    private final String installPath;
    private final Map<String, Integer> resources;
    private final String expectedBinary;
    private final String expectedConfig;
    private final String expectedStatus;

    private final static int MAX_ID_SIZE = UUID.randomUUID().toString().length();

    public static Function<SlotStatus, SlotStatusRepresentation> fromSlotStatus(final int shortIdPrefixSize, final Repository repository)
    {
        return new Function<SlotStatus, SlotStatusRepresentation>()
        {
            public SlotStatusRepresentation apply(SlotStatus status)
            {
                return from(status, shortIdPrefixSize, repository);
            }
        };
    }

    public static SlotStatusRepresentation from(SlotStatus slotStatus)
    {
        return from(slotStatus, MAX_ID_SIZE, null);
    }

    public static SlotStatusRepresentation from(SlotStatus slotStatus, int shortIdPrefixSize, Repository repository)
    {
        String binary = null;
        String config = null;
        String shortBinary = null;
        String shortConfig = null;
        if (slotStatus.getAssignment() != null) {
            binary = slotStatus.getAssignment().getBinary();
            config = slotStatus.getAssignment().getConfig();
            if (repository != null) {
                shortBinary = Objects.firstNonNull(repository.binaryRelativize(binary), binary);
                shortConfig = Objects.firstNonNull(repository.configRelativize(config), config);
            } else {
                shortBinary = binary;
                shortConfig = config;
            }

        }

        String expectedBinary = null;
        String expectedConfig = null;
        if (slotStatus.getExpectedAssignment() != null) {
            expectedBinary = slotStatus.getExpectedAssignment().getBinary();
            expectedConfig = slotStatus.getExpectedAssignment().getConfig();
        }

        String expectedStatus = null;
        if (slotStatus.getExpectedState() != null) {
            expectedStatus = slotStatus.getExpectedState().toString();
        }

        return new SlotStatusRepresentation(slotStatus.getId(),
                slotStatus.getId().toString().substring(0, shortIdPrefixSize),
                slotStatus.getName(),
                slotStatus.getSelf(),
                slotStatus.getExternalUri(),
                slotStatus.getLocation(),
                binary,
                shortBinary,
                config,
                shortConfig,
                slotStatus.getState().toString(),
                slotStatus.getVersion(),
                slotStatus.getStatusMessage(),
                slotStatus.getInstallPath(),
                slotStatus.getResources(),
                expectedBinary,
                expectedConfig,
                expectedStatus
        );
    }

    @JsonCreator
    public SlotStatusRepresentation(@JsonProperty("id") UUID id,
            @JsonProperty("shortId") String shortId,
            @JsonProperty("name") String name,
            @JsonProperty("self") URI self,
            @JsonProperty("externalUri") URI externalUri,
            @JsonProperty("location") String location,
            @JsonProperty("binary") String binary,
            @JsonProperty("shortBinary") String shortBinary,
            @JsonProperty("config") String config,
            @JsonProperty("shortConfig") String shortConfig,
            @JsonProperty("status") String status,
            @JsonProperty("version") String version,
            @JsonProperty("statusMessage") String statusMessage,
            @JsonProperty("installPath") String installPath,
            @JsonProperty("resources") Map<String, Integer> resources,
            @JsonProperty("expectedBinary") String expectedBinary,
            @JsonProperty("expectedConfig") String expectedConfig,
            @JsonProperty("expectedStatus") String expectedStatus)
    {
        this.id = id;
        this.shortId = shortId;
        this.name = name;
        this.self = self;
        this.externalUri = externalUri;
        this.location = location;
        this.binary = binary;
        this.shortBinary = shortBinary;
        this.config = config;
        this.shortConfig = shortConfig;
        this.status = status;
        this.version = version;
        this.statusMessage = statusMessage;
        this.installPath = installPath;
        this.resources = resources;
        this.expectedBinary = expectedBinary;
        this.expectedConfig = expectedConfig;
        this.expectedStatus = expectedStatus;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public UUID getId()
    {
        return id;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getShortId()
    {
        return shortId;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getName()
    {
        return name;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public URI getSelf()
    {
        return self;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public URI getExternalUri()
    {
        return externalUri;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getLocation()
    {
        return location;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getBinary()
    {
        return binary;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getShortBinary()
    {
        return shortBinary;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getConfig()
    {
        return config;
    }

    @JsonProperty
    @NotNull(message = "is missing")
    public String getShortConfig()
    {
        return shortConfig;
    }

    @JsonProperty
    public String getStatus()
    {
        return status;
    }

    @JsonProperty
    public String getVersion()
    {
        return version;
    }

    @JsonProperty
    public String getStatusMessage()
    {
        return statusMessage;
    }

    @JsonProperty
    public String getInstallPath()
    {
        return installPath;
    }

    @JsonProperty
    public Map<String, Integer> getResources()
    {
        return resources;
    }

    @JsonProperty
    public String getExpectedBinary()
    {
        return expectedBinary;
    }

    @JsonProperty
    public String getExpectedConfig()
    {
        return expectedConfig;
    }

    @JsonProperty
    public String getExpectedStatus()
    {
        return expectedStatus;
    }

    public SlotStatus toSlotStatus()
    {
        Assignment assignment = null;
        if (binary != null) {
            assignment = new Assignment(binary, config);
        }
        Assignment expectedAssignment = null;
        if (expectedBinary != null) {
            assignment = new Assignment(expectedBinary, expectedConfig);
        }
        SlotLifecycleState expectedState = null;
        if (expectedStatus != null) {
            expectedState = SlotLifecycleState.valueOf(expectedStatus);
        }
        return SlotStatus.createSlotStatusWithExpectedState(id,
                name,
                self,
                externalUri,
                location,
                SlotLifecycleState.valueOf(status),
                assignment,
                installPath,
                resources,
                expectedState,
                expectedAssignment,
                statusMessage);
    }

    public String getExternalHost() {
        if (externalUri == null) {
            return null;
        }
        return externalUri.getHost();
    }

    public String getInternalHost() {
        if (self == null) {
            return null;
        }
        return self.getHost();
    }

    public String getInternalIp()
    {
        String host = getInternalHost();
        if (host == null) {
            return null;
        }

        if ("localhost".equalsIgnoreCase(host)) {
            return "127.0.0.1";
        }

        try {
            return InetAddress.getByName(host).getHostAddress();
        }
        catch (UnknownHostException e) {
            return "unknown";
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SlotStatusRepresentation that = (SlotStatusRepresentation) o;

        if (binary != null ? !binary.equals(that.binary) : that.binary != null) {
            return false;
        }
        if (shortBinary != null ? !shortBinary.equals(that.shortBinary) : that.shortBinary != null) {
            return false;
        }
        if (config != null ? !config.equals(that.config) : that.config != null) {
            return false;
        }
        if (shortConfig != null ? !shortConfig.equals(that.shortConfig) : that.shortConfig != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (shortId != null ? !shortId.equals(that.shortId) : that.shortId != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (self != null ? !self.equals(that.self) : that.self != null) {
            return false;
        }
        if (externalUri != null ? !externalUri.equals(that.externalUri) : that.externalUri != null) {
            return false;
        }
        if (location != null ? !location.equals(that.location) : that.location != null) {
            return false;
        }
        if (status != null ? !status.equals(that.status) : that.status != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }
        if (installPath != null ? !installPath.equals(that.installPath) : that.installPath != null) {
            return false;
        }
        if (resources != null ? !resources.equals(that.resources) : that.resources != null) {
            return false;
        }
        if (expectedBinary != null ? !expectedBinary.equals(that.expectedBinary) : that.expectedBinary != null) {
            return false;
        }
        if (expectedConfig != null ? !expectedConfig.equals(that.expectedConfig) : that.expectedConfig != null) {
            return false;
        }
        if (expectedStatus != null ? !expectedStatus.equals(that.expectedStatus) : that.expectedStatus != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (shortId != null ? shortId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (binary != null ? binary.hashCode() : 0);
        result = 31 * result + (shortBinary != null ? shortBinary.hashCode() : 0);
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (shortConfig != null ? shortConfig.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (self != null ? self.hashCode() : 0);
        result = 31 * result + (externalUri != null ? externalUri.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (installPath != null ? installPath.hashCode() : 0);
        result = 31 * result + (resources != null ? resources.hashCode() : 0);
        result = 31 * result + (expectedBinary != null ? expectedBinary.hashCode() : 0);
        result = 31 * result + (expectedConfig != null ? expectedConfig.hashCode() : 0);
        result = 31 * result + (expectedStatus != null ? expectedStatus.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("SlotStatusRepresentation");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", shortId='").append(shortId).append('\'');
        sb.append(", self=").append(self);
        sb.append(", externalUri=").append(externalUri);
        sb.append(", location=").append(location);
        sb.append(", binary='").append(binary).append('\'');
        sb.append(", shortBinary='").append(shortBinary).append('\'');
        sb.append(", config='").append(config).append('\'');
        sb.append(", shortConfig='").append(shortConfig).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", statusMessage='").append(statusMessage).append('\'');
        sb.append(", installPath='").append(installPath).append('\'');
        sb.append(", resources='").append(resources).append('\'');
        sb.append(", expectedBinary='").append(expectedBinary).append('\'');
        sb.append(", expectedConfig='").append(expectedConfig).append('\'');
        sb.append(", expectedStatus='").append(expectedStatus).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
