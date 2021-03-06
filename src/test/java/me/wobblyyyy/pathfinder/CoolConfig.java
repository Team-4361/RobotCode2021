package me.wobblyyyy.pathfinder;

import me.wobblyyyy.pathfinder.config.PathfinderConfig;
import me.wobblyyyy.pathfinder.followers.Followers;
import me.wobblyyyy.pathfinder.util.RobotProfile;

public class CoolConfig extends PathfinderConfig {
    static CoolMotor m = new CoolMotor();

    public CoolConfig() {
        super(
                new CoolOdometry(),
                323,
                629,
                2,
                10,
                10,
                5,
                5,
                new RobotProfile(1, 1, 15, 15, 5, 0),
                new CoolDrive(
                        m, m, m, m,
                        m, m, m, m,
                        m, m, m, m,
                        m, m, m, m
                ),
                new CoolMap(),
                Followers.SWERVE,
                true,
                true,
                true
        );
    }
}
