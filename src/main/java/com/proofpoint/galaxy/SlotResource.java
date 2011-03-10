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
package com.proofpoint.galaxy;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static com.proofpoint.galaxy.SlotStatusRepresentation.getSelfUri;

@Path("/v1/slot")
public class SlotResource
{
    private final AgentManager agentManager;

    @Inject
    public SlotResource(AgentManager agentManager)
    {
        Preconditions.checkNotNull(agentManager, "slotsManager must not be null");

        this.agentManager = agentManager;
    }

    @POST
    public Response addSlot(@Context UriInfo uriInfo)
    {
        SlotManager slotManager = agentManager.addNewSlot();
        return Response.created(getSelfUri(uriInfo, slotManager.getName())).build();
    }

    @Path("{slotName: [a-z0-9]+}")
    @DELETE
    public Response removeSlot(@PathParam("slotName") String id)
    {
        Preconditions.checkNotNull(id, "id must not be null");

        if (!agentManager.deleteSlot(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.noContent().build();
    }

    @Path("{slotName: [a-z0-9]+}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSlotStatus(@PathParam("slotName") String slotName, @Context UriInfo uriInfo)
    {
        Preconditions.checkNotNull(slotName, "slotName must not be null");

        SlotManager slotManager = agentManager.getSlot(slotName);
        if (slotManager == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("[" + slotName + "]").build();
        }

        SlotStatus slotStatus = slotManager.status();
        return Response.ok(SlotStatusRepresentation.from(slotStatus, uriInfo)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllSlotsStatus(@Context UriInfo uriInfo)
    {
        List<SlotStatusRepresentation> representations = Lists.newArrayList();
        for (SlotManager slotManager : agentManager.getAllSlots()) {
            SlotStatus slotStatus = slotManager.status();
            representations.add(SlotStatusRepresentation.from(slotStatus, uriInfo));
        }
        return Response.ok(representations).build();
    }
}
