package com.abilix.robot.walktunner;

/**
 * @author wanghang
 * Created on 17-7-1.
 *
 * change wudong
 */

public class KalmanFilter {
    private float Angle_Balance,Accel_Angle;
    private float angleR, angle_dot;
    private double Q_angle = 0.001;
    private double Q_gyro = 0.003;
    private double R_angle = 0.5;
    private double dt = 0.1;
    private char C_0 = 1;
    private float Q_bias, Angle_err;
    private float PCt_0, PCt_1, E;
    private float K_0, K_1, t_0, t_1;
    private float[] Pdot = {0, 0, 0, 0};
    private float[][] PP_new = {{1, 0}, {0, 1}};

    public float Get_Angle(float gyroX, float accelY, float accelZ) {
        Accel_Angle = (float) (Math.atan2(accelY, accelZ) * 180 / (3.1415926));
        Kalman_Filter(Accel_Angle, gyroX);
        Angle_Balance = angleR;
        return Angle_Balance;
    }

    private void Kalman_Filter(float Accel, float Gyro) {
        angleR += (Gyro - Q_bias) * dt;
        Pdot[0] = (float) (Q_angle - PP_new[0][1] - PP_new[1][0]);

        Pdot[1] = -PP_new[1][1];
        Pdot[2] = -PP_new[1][1];
        Pdot[3] = (float) Q_gyro;
        PP_new[0][0] += Pdot[0] * dt;
        PP_new[0][1] += Pdot[1] * dt;
        PP_new[1][0] += Pdot[2] * dt;
        PP_new[1][1] += Pdot[3] * dt;

        Angle_err = Accel - angleR;

        PCt_0 = C_0 * PP_new[0][0];
        PCt_1 = C_0 * PP_new[1][0];

        E = (float) (R_angle + C_0 * PCt_0);

        K_0 = PCt_0 / E;
        K_1 = PCt_1 / E;

        t_0 = PCt_0;
        t_1 = C_0 * PP_new[0][1];

        PP_new[0][0] -= K_0 * t_0;
        PP_new[0][1] -= K_0 * t_1;
        PP_new[1][0] -= K_1 * t_0;
        PP_new[1][1] -= K_1 * t_1;

        angleR += K_0 * Angle_err;
        Q_bias += K_1 * Angle_err;
        angle_dot = Gyro - Q_bias;
    }
}
