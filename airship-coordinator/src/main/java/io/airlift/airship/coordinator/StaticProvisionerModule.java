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
package io.airlift.airship.coordinator;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import io.airlift.airship.coordinator.auth.AuthorizedKeyStore;
import io.airlift.airship.coordinator.auth.FileAuthorizedKeyStore;
import io.airlift.airship.coordinator.auth.FileAuthorizedKeyStoreConfig;
import io.airlift.configuration.ConfigurationModule;

public class StaticProvisionerModule
        implements Module
{
    public void configure(Binder binder)
    {
        binder.disableCircularProxies();
        binder.requireExplicitBindings();

        binder.bind(Provisioner.class).to(StaticProvisioner.class).in(Scopes.SINGLETON);
        ConfigurationModule.bindConfig(binder).to(StaticProvisionerConfig.class);

        binder.bind(StateManager.class).to(FileStateManager.class).in(Scopes.SINGLETON);
        ConfigurationModule.bindConfig(binder).to(FileStateManagerConfig.class);

        binder.bind(AuthorizedKeyStore.class).to(FileAuthorizedKeyStore.class).in(Scopes.SINGLETON);
        ConfigurationModule.bindConfig(binder).to(FileAuthorizedKeyStoreConfig.class);
    }
}
