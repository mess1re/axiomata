package me.mss1r.axiomata.ballistics;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProjectileDirectDamageTest {
    @Test
    void reloadedProfileOverridesReferenceSpeedWithoutLosingLaunchFallback() {
        var projectile = mock(BallisticProjectile.class, CALLS_REAL_METHODS);
        doReturn(ProjectilePhysicsProfile.DEFAULT).when(projectile).getPhysicsProfile();
        projectile.setReferenceSpeed(400);
        assertEquals(400, projectile.getReferenceSpeed());

        var override = ProjectilePhysicsProfile.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"directHit\":{\"damage\":50,\"referenceSpeed\":200}}"))
                .result().orElseThrow();
        doReturn(override).when(projectile).getPhysicsProfile();
        assertEquals(200, projectile.getReferenceSpeed());

        doReturn(ProjectilePhysicsProfile.DEFAULT).when(projectile).getPhysicsProfile();
        assertEquals(400, projectile.getReferenceSpeed());
    }
}
