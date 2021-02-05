package de.tu_bs.iff.adsb.dataparser.cosmath;

/**
 * CosMath (CoordinateSystem-Math) is a collection of mathematical methods especially in the field of coordinate-systems. 
 * @author LLS
 */
public class CosMath {
	public static final double DEG_TO_RAD = Math.PI/180;
	public static final double RAD_TO_DEG = 180/Math.PI;
	
	public static final double FEET_TO_METER = 0.3048;
	public static final double METER_TO_FEET = 1/FEET_TO_METER;
	
	public static final double NM_TO_KM = 1.852;
	public static final double KM_TO_NM = 1/NM_TO_KM;
	
	public static final double CONST_f = 1/298.257223563;
	public static final double CONST_a = 6378.137;
	
	/**
	 * Calculates the shortest distance between posA and posB on earth. Unit: km
	 * Reference: Meeus, Jean: Astronomische Algorithmen. 2. Auflage. Barth, Leipzig 1994, ISBN 3-335-00400-0
	 * @param posA Position A, unit: rad {latA, lonA}
	 * @param posB Position B, unit: rad {latB, lonB}
	 * @return Distance, unit: km
	 */
	public static double earthDist(double[] posA, double[] posB) {
		if((posA[0] == posB[0]) && (posA[1] == posB[1]))
			return 0;

		double f = (posA[0]+posB[0])/2;
		double g = (posA[0]-posB[0])/2;
		double l = (posA[1]-posB[1])/2;

		double s = Math.pow(Math.sin(g),2)*Math.pow(Math.cos(l),2)+Math.pow(Math.cos(f),2)*Math.pow(Math.sin(l),2);
		double c = Math.pow(Math.cos(g),2)*Math.pow(Math.cos(l),2)+Math.pow(Math.sin(f),2)*Math.pow(Math.sin(l),2);
		double w = Math.atan(Math.sqrt(s/c));
		double d = 2*w*CONST_a;

		double t = Math.sqrt(s*c)/w;
		double h1 = (3*t-1)/(2*c);
		double h2 = (3*t+1)/(2*s);

		double dist = d*(1+CONST_f*h1*Math.pow(Math.sin(f),2)*Math.pow(Math.cos(g),2)-CONST_f*h2*Math.pow(Math.cos(f),2)*Math.pow(Math.sin(g),2));
		return dist;
	}
	
	/**
	 * Estimates the shortest distance between posA and posB on earth. Unit: NM
	 * @param posA Position A, unit: rad {latA, lonA}
	 * @param posB Position B, unit: rad {latB, lonB}
	 * @return Distance, unit: NM
	 */
	public static double earthDistEstimatedNM(double[] posA, double[] posB) {
		final double NM_PER_ORTHODROME_ANGLE = 60;

		double deltaLat = (posB[0]-posA[0])*CosMath.RAD_TO_DEG;
		double deltaLon = (posB[1]-posA[1])*CosMath.RAD_TO_DEG;
		double deltaLonCorrected = deltaLon*Math.cos((posA[0]+posB[0])/2);

		double angleDistanceCorrected = Math.sqrt(Math.pow(deltaLat,2) + Math.pow(deltaLonCorrected,2));
		return angleDistanceCorrected*NM_PER_ORTHODROME_ANGLE;
	}
	
	public static double[] posDegToRad(double[] position) {
		double[] positionRad = new double[2];
		positionRad[0] = position[0]*DEG_TO_RAD;
		positionRad[1] = position[1]*DEG_TO_RAD;
		return positionRad;
	}
	
	public static double[] posRadToDeg(double[] position) {
		double[] positionDeg = new double[2];
		positionDeg[0] = position[0]*RAD_TO_DEG;
		positionDeg[1] = position[1]*RAD_TO_DEG;
		return positionDeg;
	}

	/**
	 * Calculates the length of a vector in cartesian COS
	 * @param vect Vector {va, vb, vc}
	 * @return Length of vector
	 */
	public static double vectLength(double[] vect) {
		return Math.sqrt(Math.pow(vect[0],2)+Math.pow(vect[1],2)+Math.pow(vect[2],2));
	}

	/**
	 * Calculates the length of a 2D-vector in cartesian COS
	 * @param vect Vector {va, vb}
	 * @return Length of vector
	 */
	public static double vect2DLength(double[] vect) {
		return Math.sqrt(Math.pow(vect[0],2)+Math.pow(vect[1],2));
	}
	
	/**
	 * Calculates the angle between two vectors in cartesian COS. Unit: rad
	 * @param vectA First vector {va, vb, vc}
	 * @param vectB Second vector {wa, wb, wc}
	 * @return Angle between vectors, unit: rad
	 */
	public static double kartesianAngle(double[] vectA, double[] vectB) {
		double angle = Math.acos((vectA[0]*vectB[0]+vectA[1]*vectB[1]+vectA[2]*vectB[2]) / (vectLength(vectA)*vectLength(vectB)));
		return angle;
	}

	/**
	 * Calculates the angle between two vectors in 2D-cartesian COS. Unit: rad
	 * @param vectA First vector {va, vb}
	 * @param vectB Second vector {wa, wb}
	 * @return Angle between vectors, unit: rad (<0: anti-clockwise; >0: clockwise)
	 */
	public static double kartesian2DAngle(double[] vectA, double[] vectB) {
		if((vect2DLength(vectA) == 0) || (vect2DLength(vectB) == 0))
			return 0;
		double angle = Math.acos((vectA[0]*vectB[0]+vectA[1]*vectB[1]) / (vect2DLength(vectA)*vect2DLength(vectB)));
		double crossProductZ = vectA[0]*vectB[1]-vectB[0]*vectA[1];
		if(crossProductZ > 0)
			angle = -angle;
		return angle;
	}
	
	/**
	 * Rotates a vector in cartesian COS around X-axis
	 * @param vect Origin-vector {va, vb, vc}
	 * @param angle Angle for rotating origin-vector, unit: rad
	 * @return Rotated vector {wa, wb, wc}
	 */
	public static double[] rotateX(double[] vect, double angle) {
		double[] vectRotated = new double[3];
		vectRotated[0] = vect[0];
		vectRotated[1] = Math.cos(angle)*vect[1]+Math.sin(angle)*vect[2];
		vectRotated[2] = -Math.sin(angle)*vect[1]+Math.cos(angle)*vect[2];
		
		return vectRotated;
	}

	/**
	 * Rotates a vector in cartesian COS around Y-axis
	 * @param vect Origin-vector {va, vb, vc}
	 * @param angle Angle for rotating origin-vector, unit: rad
	 * @return Rotated vector {wa, wb, wc}
	 */
	public static double[] rotateY(double[] vect, double angle) {
		double[] vectRotated = new double[3];
		vectRotated[0] = Math.cos(angle)*vect[0]-Math.sin(angle)*vect[2];
		vectRotated[1] = vect[1];
		vectRotated[2] = Math.sin(angle)*vect[0]+Math.cos(angle)*vect[2];
		
		return vectRotated;
	}

	/**
	 * Rotates a vector in cartesian COS around Z-axis
	 * @param vect Origin-vector {va, vb, vc}
	 * @param angle Angle for rotating origin-vector, unit: rad
	 * @return Rotated vector {wa, wb, wc}
	 */
	public static double[] rotateZ(double[] vect, double angle) {
		double[] vectRotated = new double[3];
		vectRotated[0] = Math.cos(angle)*vect[0]+Math.sin(angle)*vect[1];
		vectRotated[1] = -Math.sin(angle)*vect[0]+Math.cos(angle)*vect[1];
		vectRotated[2] = vect[2];
		
		return vectRotated;
	}

	/**
	 * Rotates a 2D-vector in cartesian COS (around imaginary Z-axis)
	 * @param vect Origin-vector {va, vb}
	 * @param angle Angle for rotating origin-vector, unit: rad
	 * @return Rotated vector {wa, wb}
	 */
	public static double[] rotate2D(double[] vect, double angle) {
		double[] vectRotated = new double[2];
		vectRotated[0] = Math.cos(angle)*vect[0]+Math.sin(angle)*vect[1];
		vectRotated[1] = -Math.sin(angle)*vect[0]+Math.cos(angle)*vect[1];
		
		return vectRotated;
	}
	
	/**
	 * Transforms a cartesian vector into the polar COS
	 * @param vectCartesian Vector in cartesian COS {xa, ya, za}
	 * @return Vector in polar COS, angle-unit: rad {Theta, phi, R}
	 */
	public static double[] transformKartesianPolar(double[] vectCartesian) {
		// phi: longitude
		double phi = kartesianAngle(new double[] {0, 1, 0}, new double[] {vectCartesian[0], vectCartesian[1], 0});
		if(vectCartesian[0] > 0)
			phi = -phi;
		// Theta: latitude
		double Theta = kartesianAngle(new double[] {0, 0, 1}, vectCartesian);
		Theta = Math.PI/2 - Theta;
		// R: radius
		double R = vectLength(vectCartesian);

		return new double[] {Theta, phi, R};
	}
	
	/**
	 * Transforms a polar vector into the cartesian COS
	 * @param vectPolar Vector in polar COS, angle-unit: rad {Theta, phi, R}
	 * @return Vector in cartesian COS {xa, ya, za}
	 */
	public static double[] transformPolarKartesian(double[] vectPolar) {
		double z = vectPolar[2]*Math.sin(vectPolar[0]);
		double rXY = vectPolar[2]*Math.cos(vectPolar[0]);
		double y = rXY*Math.cos(vectPolar[1]);
		double x = -rXY*Math.sin(vectPolar[1]);
		
		return new double[] {x, y, z};
	}

	/**
	 * Interpolates a position on the shortest part of the orthodrome between two positions
	 * @param posA First position, unit: rad {latA, lonA}
	 * @param posB Second position, unit: rad {latB, lonB}
	 * @param relativePosition Relative position (0 ... 1) between position posA and posB for interpolation
	 * @return Interpolated position on the orthodrome between posA and posB, unit: rad {lat, lon}
	 */
	public static double[] orthodromeInterpolate(double[] posA, double[] posB, double relativePosition) {
		// R: Temporary radius for polar coordinates
		final double R = 1;

		// transform to cartesian coordinates: 
		double[] posAcalc = transformPolarKartesian(new double[] {posA[0], posA[1], R});
		double[] posBcalc = transformPolarKartesian(new double[] {posB[0], posB[1], R});

		double angleZ = kartesianAngle(new double[] {0, 1, 0}, new double[] {posAcalc[0], posAcalc[1], 0});

		if(posAcalc[0] < 0)
			angleZ = -angleZ;

		// rotate around Z-axis, to bring posAcalc into the YZ-plane
		posAcalc = rotateZ(posAcalc, -angleZ);
		posBcalc = rotateZ(posBcalc, -angleZ);

		double angleX = kartesianAngle(new double[] {0, 0, 1}, posAcalc);

		if(posAcalc[1] < 0)
			angleX = -angleX;

		// rotate around X-axis, to rotate posAcalc onto the Z-axis
		posAcalc = rotateX(posAcalc, -angleX);
		posBcalc = rotateX(posBcalc, -angleX);

		double angleBZ = kartesianAngle(new double[] {0, 1, 0}, new double[] {posBcalc[0], posBcalc[1], 0});

		if(posBcalc[0] < 0)
			angleBZ = -angleBZ;

		// rotate around Z-axis, to bring posBcalc in the YZ-plane
		posAcalc = rotateZ(posAcalc, -angleBZ);
		posBcalc = rotateZ(posBcalc, -angleBZ);

		// posAcalc and posBcalc now are in the YZ-plane, whereby posAcalc points into Z-axis-direction
		// Therefore the orthodrome between posAcalc and posBcalc is also in the YZ-plane
		// Calculation of aperture-angle between posA and posB respectively posAcalc and posBcalc:
		double angleAB = kartesianAngle(posAcalc, posBcalc);

		int posBturnRight = 1;
		if(posBcalc[1] < 0)
			posBturnRight = -1;
		
		// individual part ...

		double angleABnew = angleAB * relativePosition;

		double[] posBnew = {0, posBturnRight*R*Math.sin(angleABnew), R*Math.cos(angleABnew)};

		// Back-transformation of rotation in reverse order ...
		posBnew = rotateZ(posBnew, angleBZ);
		posBnew = rotateX(posBnew, angleX);
		posBnew = rotateZ(posBnew, angleZ);

		// Back-transformation in polar coordinates
		double[] posBnewPolar = transformKartesianPolar(posBnew);
		
		return new double[] {posBnewPolar[0], posBnewPolar[1]};
	}
	
	/**
	 * Initializes/Calculates the general values for interpolating a position on the shortest part of the orthodrome between two positions. 
	 * The calculations within -PartInit only need to be calculated per value-pair posA and posB, resulting by a sequential calculation within -PartInit and -PartCalc. 
	 * @param posA First position, unit: rad {latA, lonA}
	 * @param posB Second position, unit: rad {latB, lonB}
	 * @return Init-values for interpolating on a orthodrome between posA and posB
	 */
	public static double[] orthodromeInterpolatePartInit(double[] posA, double[] posB) {
		// R: Temporary radius for polar coordinates
		final double R = 1;

		// transform to cartesian coordinates: 
		double[] posAcalc = transformPolarKartesian(new double[] {posA[0], posA[1], R});
		double[] posBcalc = transformPolarKartesian(new double[] {posB[0], posB[1], R});

		double angleZ = kartesianAngle(new double[] {0, 1, 0}, new double[] {posAcalc[0], posAcalc[1], 0});

		if(posAcalc[0] < 0)
			angleZ = -angleZ;

		// rotate around Z-axis, to bring posAcalc into the YZ-plane
		posAcalc = rotateZ(posAcalc, -angleZ);
		posBcalc = rotateZ(posBcalc, -angleZ);

		double angleX = kartesianAngle(new double[] {0, 0, 1}, posAcalc);

		if(posAcalc[1] < 0)
			angleX = -angleX;

		// rotate around X-axis, to rotate posAcalc onto the Z-axis
		posAcalc = rotateX(posAcalc, -angleX);
		posBcalc = rotateX(posBcalc, -angleX);

		double angleBZ = kartesianAngle(new double[] {0, 1, 0}, new double[] {posBcalc[0], posBcalc[1], 0});

		if(posBcalc[0] < 0)
			angleBZ = -angleBZ;

		// rotate around Z-axis, to bring posBcalc in the YZ-plane
		posAcalc = rotateZ(posAcalc, -angleBZ);
		posBcalc = rotateZ(posBcalc, -angleBZ);

		// posAcalc and posBcalc now are in the YZ-plane, whereby posAcalc points into Z-axis-direction
		// Therefore the orthodrome between posAcalc and posBcalc is also in the YZ-plane
		// Calculation of aperture-angle between posA and posB respectively posAcalc and posBcalc:
		double angleAB = kartesianAngle(posAcalc, posBcalc);

		int posBturnRight = 1;
		if(posBcalc[1] < 0)
			posBturnRight = -1;
		
		return new double[] {angleAB, posBturnRight, angleBZ, angleX, angleZ};
		// individual part ...
	}

	/**
	 * Second/Individual part for interpolating a position on the shortest part of the orthodrome between two positions. 
	 * The calculations within -PartInit only need to be calculated per value-pair posA and posB, resulting by a sequential calculation within -PartInit and -PartCalc. 
	 * @param partInitValues Init-values for interpolating on a orthodrome between posA and posB (see orthodromeInterpolatePartInit(double[] posA, double[] posB))
	 * @param relativePosition Relative position (0 ... 1) between position posA and posB for interpolation
	 * @return Interpolated position on the orthodrome between posA and posB, unit: rad {lat, lon}
	 */
	public static double[] orthodromeInterpolatePartCalc(double[] partInitValues, double relativePosition) {
		// R: Temporary radius for polar coordinates
		final double R = 1;
		// individual part ...
		
		double angleAB = partInitValues[0];
		double posBturnRight = partInitValues[1];
		double angleBZ = partInitValues[2];
		double angleX = partInitValues[3];
		double angleZ = partInitValues[4];

		double angleABnew = angleAB * relativePosition;

		double[] posBnew = {0, posBturnRight*R*Math.sin(angleABnew), R*Math.cos(angleABnew)};

		// Back-transformation of rotation in reverse order ...
		posBnew = rotateZ(posBnew, angleBZ);
		posBnew = rotateX(posBnew, angleX);
		posBnew = rotateZ(posBnew, angleZ);

		// Back-transformation in polar coordinates
		double[] posBnewPolar = transformKartesianPolar(posBnew);
		
		return new double[] {posBnewPolar[0], posBnewPolar[1]};
	}
	
	/**
	 * Determines the nearest Position (return value) on an orthodrome (orthodromePosA, orthodromePosB) to the Position pos
	 * @param pos Position beneath the orthodrome {lat, lon} in rad
	 * @param orthodromePosA Position A on the orthodrome {lat, lon} in rad
	 * @param orthodromePosB Position B on the orthodrome {lat, lon} in rad
	 * @return Nearest position to the orthodrome {lat, lon} in rad
	 */
	public static double[] orthodromeNearestPosition(double[] pos, double[] orthodromePosA, double[] orthodromePosB) {
		final double R = 1;		// temporary sphere-radius (dissolves itself)
		
		double[] nearestPosOnOrthodrome = new double[2];
		if(orthodromePosA[0] == orthodromePosB[0])
			if(orthodromePosA[1] == orthodromePosB[1]) {
				// the orthodrome-positions are equal --> orthodrome is arbitrary
				nearestPosOnOrthodrome[0] = pos[0];
				nearestPosOnOrthodrome[1] = pos[1];
				return nearestPosOnOrthodrome;
			}
		
		double[] posAkartesian = transformPolarKartesian(new double[] {orthodromePosA[0], orthodromePosA[1], R});
		double[] posBkartesian = transformPolarKartesian(new double[] {orthodromePosB[0], orthodromePosB[1], R});
		double[] posKartesian = transformPolarKartesian(new double[] {pos[0], pos[1], R});
		
		double[] nVector = new double[3];
		nVector[0] = posAkartesian[1]*posBkartesian[2]-posAkartesian[2]*posBkartesian[1];
		nVector[1] = posAkartesian[2]*posBkartesian[0]-posAkartesian[0]*posBkartesian[2];
		nVector[2] = posAkartesian[0]*posBkartesian[1]-posAkartesian[1]*posBkartesian[0];
		
		double lineRelativePosition = -(posKartesian[0]*nVector[0] + posKartesian[1]*nVector[1] + posKartesian[2]*nVector[2]) / (Math.pow(nVector[0],2) + Math.pow(nVector[1],2) + Math.pow(nVector[2],2));
		
		double[] nearestPosOnOrthodromeKartesian = new double[3];
		
		nearestPosOnOrthodromeKartesian[0] = posKartesian[0] + lineRelativePosition*nVector[0];
		nearestPosOnOrthodromeKartesian[1] = posKartesian[1] + lineRelativePosition*nVector[1];
		nearestPosOnOrthodromeKartesian[2] = posKartesian[2] + lineRelativePosition*nVector[2];
		
		double[] nearestPosOnOrthodromePolar = transformKartesianPolar(nearestPosOnOrthodromeKartesian);
		
		nearestPosOnOrthodrome[0] = nearestPosOnOrthodromePolar[0];
		nearestPosOnOrthodrome[1] = nearestPosOnOrthodromePolar[1];
		
		return nearestPosOnOrthodrome;
	}

	/**
	 * Determines the track-angle at relativePosition on an orthodrome between posA and posB
	 * @param posA Position A on the orthodrome {lat, lon} in rad
	 * @param posB Position B on the orthodrome {lat, lon} in rad
	 * @param relativePosition Relative position (0 ... 1) between posA and posB
	 * @return Track-angle in rad
	 */
	public static double calcOrthodromeTrackAngle(double[] posA, double[] posB, double relativePosition) {
		double trackAngle;

		double[] leftPos = posA;
		double[] rightPos = posB;

		double[] leftDeltaPos;
		double[] rightDeltaPos;
		if(CosMath.earthDistEstimatedNM(leftPos, rightPos) > 4) {
			double relativeDelta = 2/CosMath.earthDistEstimatedNM(leftPos, rightPos);
			double leftRelativePosition = relativePosition - relativeDelta;
			double rightRelativePosition = relativePosition + relativeDelta;
			if(leftRelativePosition > 0)
				leftDeltaPos = CosMath.orthodromeInterpolate(leftPos, rightPos, leftRelativePosition);
			else
				leftDeltaPos = new double[] {leftPos[0], leftPos[1]};
			if(rightRelativePosition < 1)
				rightDeltaPos = CosMath.orthodromeInterpolate(leftPos, rightPos, rightRelativePosition);
			else
				rightDeltaPos = new double[] {rightPos[0], rightPos[1]};
		} else {
			leftDeltaPos = new double[] {leftPos[0], leftPos[1]};
			rightDeltaPos = new double[] {rightPos[0], rightPos[1]};
		}

		double deltaLat = (rightDeltaPos[0]-leftDeltaPos[0])*CosMath.RAD_TO_DEG;
		double deltaLon = (rightDeltaPos[1]-leftDeltaPos[1])*CosMath.RAD_TO_DEG;

		double deltaLonCorrected = deltaLon*Math.cos((leftDeltaPos[0]+rightDeltaPos[0])/2);
		if(Math.abs(deltaLat) == 0)
			trackAngle = Math.PI/2;
		else
			trackAngle = Math.atan(Math.abs(deltaLonCorrected/deltaLat));
		if(deltaLat < 0) {
			if(deltaLonCorrected < 0)
				trackAngle += Math.PI;
			else
				trackAngle = Math.PI-trackAngle;
		} else
			if(deltaLonCorrected < 0)
				trackAngle = 2*Math.PI - trackAngle;

		return trackAngle;
	}

}
