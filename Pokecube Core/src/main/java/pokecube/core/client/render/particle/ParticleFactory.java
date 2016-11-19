package pokecube.core.client.render.particle;

import thut.api.maths.Vector3;
import thut.api.maths.Vector4;

public class ParticleFactory
{
    public static IParticle makeParticle(String name, Vector3 velocity, int... args)
    {
        IParticle ret = null;
        if (name.equalsIgnoreCase("string"))
        {
            ParticleNoGravity particle = new ParticleNoGravity(8, 5);
            particle.setVelocity(velocity);
            ret = particle;
        }
        else if (name.equalsIgnoreCase("aurora"))
        {
            ParticleNoGravity particle = new ParticleNoGravity(0, 0);
            particle.setVelocity(velocity);
            int[][] textures = new int[2][2];
            textures[0][0] = 2;
            textures[0][1] = 4;
            textures[1][0] = 1;
            textures[1][1] = 4;
            particle.setTex(textures);
            particle.name = "aurora";
            int life = 32;
            if (args.length > 1) life = args[1];
            particle.setLifetime(life);
            ret = particle;
        }
        else if (name.equalsIgnoreCase("misc"))
        {
            ParticleNoGravity particle = new ParticleNoGravity(0, 0);
            particle.setVelocity(velocity);
            int[][] textures = new int[2][2];
            textures[0][0] = 2;
            textures[0][1] = 4;
            textures[1][0] = 1;
            textures[1][1] = 4;
            particle.setTex(textures);
            particle.name = "misc";
            int life = 32;
            if (args.length > 0) particle.setColour(args[0]);
            if (args.length > 0) life = args[1];
            particle.setLifetime(life);
            particle.setSize(0.25f);
            ret = particle;
        }
        else if (name.equalsIgnoreCase("powder"))
        {
            ParticleNoGravity particle = new ParticleNoGravity(0, 0);
            particle.setVelocity(velocity);
            int[][] textures = new int[7][2];
            textures[0][0] = 0;
            textures[0][1] = 0;
            textures[1][0] = 1;
            textures[1][1] = 0;
            textures[2][0] = 2;
            textures[2][1] = 0;
            textures[3][0] = 3;
            textures[3][1] = 0;
            textures[4][0] = 4;
            textures[4][1] = 0;
            textures[5][0] = 5;
            textures[5][1] = 0;
            textures[6][0] = 6;
            textures[6][1] = 0;
            particle.setTex(textures);
            particle.setSize(0.125f);
            particle.name = "powder";
            int life = 32;
            if (args.length > 0) particle.setColour(args[0]);
            if (args.length > 1) life = args[1];
            particle.setLifetime(life);
            ret = particle;
        }
        else if (name.equalsIgnoreCase("leaf"))
        {
            ParticleOrientable particle = new ParticleOrientable(2, 2);
            particle.setLifetime(20);
            particle.setVelocity(velocity);
            particle.size = 0.25;
            if (velocity != null)
            {
                Vector3 normal = velocity.normalize().copy();
                Vector4 v3 = new Vector4(0, 1, 0, (float) (90 - normal.toSpherical().z * 180 / Math.PI));
                Vector4 v2 = new Vector4(1, 0, 0, (float) (90 + (normal.y * 180 / Math.PI)));
                particle.setOrientation(v3.addAngles(v2));
            }
            ret = particle;
        }
        else
        {
            ParticleNoGravity particle = new ParticleNoGravity(0, 0);
            particle.setVelocity(velocity);
            ret = particle;
        }
        return ret;
    }
}
