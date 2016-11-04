/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.stickable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.inventory.PickupComponent;
import org.terasology.math.geom.Vector3i;
import org.terasology.physics.events.ImpactEvent;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
public class StickableSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    private static final Logger logger = LoggerFactory.getLogger(StickableSystem.class);

    @In
    private WorldProvider worldProvider;
    @In
    private EntityManager entityManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;

    @Override
    public void update(float delta) {
        for (EntityRef entity : entityManager.getEntitiesWith(PickupComponent.class)) {

            StickableComponent stickableComponent = entity.getComponent(StickableComponent.class);
            if ( stickableComponent == null )
            {
                stickableComponent = new StickableComponent();
                entity.addComponent(stickableComponent);
            }
        }
    }

    @ReceiveEvent(components = {StickableComponent.class})
    public void onItemImpact(ImpactEvent event, EntityRef entity) {
        //place block properly
        BlockItemComponent blockItem = entity.getComponent(BlockItemComponent.class);
        if (blockItem == null) { return; }
        BlockFamily type = blockItem.blockFamily;

        EntityRef hitEntity = event.getHitEntity();
        if (hitEntity == null){ return; }

        BlockComponent blockComponent = hitEntity.getComponent(BlockComponent.class);
        Vector3i targetBlock = blockComponent.getPosition();
        Vector3i placementPos = new Vector3i(targetBlock);
        placementPos.add(event.getSide().getVector3i());

        if (!worldProvider.getBlock(placementPos).isReplacementAllowed()) { return; }
        Block block = type.getBlockForPlacement(worldProvider, blockEntityRegistry, placementPos, event.getSide(), event.getSide());

        PlaceBlocks placeBlocks = new PlaceBlocks(placementPos, block, EntityRef.NULL);
        worldProvider.getWorldEntity().send(placeBlocks);

        entity.destroy();
    }
}
