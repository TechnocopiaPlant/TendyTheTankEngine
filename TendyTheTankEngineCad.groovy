import com.neuronrobotics.bowlerkernel.Bezier3d.*
import com.neuronrobotics.bowlerkernel.Bezier3d.BezierEditor

import com.neuronrobotics.bowlerstudio.creature.ICadGenerator
import com.neuronrobotics.bowlerstudio.physics.TransformFactory
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
import eu.mihosoft.vrl.v3d.*
import eu.mihosoft.vrl.v3d.Vector3d
import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.parametrics.*
import marytts.signalproc.sinusoidal.TrackModifier


return new ICadGenerator(){
	
	// Load ForkyLiftCad.groovy script from TechnocopiaPlant/ForkyRobot repository
	ICadGenerator lift = ScriptingEngine.gitScriptRun('https://github.com/TechnocopiaPlant/ForkyRobot.git', 'ForkyLiftCad.groovy')
	private ArrayList<CSG> baseLinkCAD = null
	
	public getBaseLinkCAD(kinematics){
		
		if(baseLinkCAD==null) {
			baseLinkCAD = lift.generateCad(kinematics, 0)
		}
		return baseLinkCAD
	}

	@Override
	public ArrayList<CSG> generateCad(DHParameterKinematics kinematics, int linkIndex) {
		ArrayList<CSG> back = []
		ArrayList<CSG> liftImportCAD = []
		if (linkIndex == 0) {
			liftImportCAD = getBaseLinkCAD(kinematics)
		}
		else {
			liftImportCAD = lift.generateCad(kinematics, linkIndex)
		}
		ArrayList<CSG> liftCAD = []
		
		for (CSG c : liftImportCAD) {
		    String name = c.getName()
		    if (name != null && !name.toLowerCase().contains("bucket")) {
		        liftCAD.add(c)
		    }
		}
		
		back.addAll(liftCAD)
		return back
	}

	@Override
	public ArrayList<CSG> generateBody(MobileBase arg0) {
		
		// Initialize an empty ArrayList to hold the generated CSG objects
		ArrayList<CSG> back =[]
		
		def production = false
		
		// Define URL link to the GitHub repo for this robot
		def URL="https://github.com/TechnocopiaPlant/TendyTheTankEngine.git"
		
		// define the parameters for the flat stock material
		//LengthParameter boardThickness = new LengthParameter('Board Thickness (mm)', 3, [0, 100])			// laser cut wood thickness
		LengthParameter boardThickness = new LengthParameter('Board Thickness (mm)', 19, [0, 100])			// 4'x8' plywood sheets
		boardThickness.setMM(19)
		
		// define the parameters for the overall size of the bay itself
		LengthParameter bayDepth = new LengthParameter("Bay Depth (mm)", 400, [0, 1000])
		//bayDepth.setMM(379.5)
		//bayDepth.setMM(390)
		//bayDepth.setMM(400)
		//bayDepth.setMM(420)
		//bayDepth.setMM(440)
		bayDepth.setMM(700)
		LengthParameter bayWidth = new LengthParameter("Bay Width (mm)", 400, [0, 1000])
		bayWidth.setMM(500)
		LengthParameter bayHeight = new LengthParameter("Bay Height (mm)", 1300, [0, 5000])
		bayHeight.setMM(1400)
		
		// define the parameters for the shelf that holds the plant
		def armBezierPieces = 12
		LengthParameter bucketDiameter = new LengthParameter("Bucket Diameter (mm)", 304.8, [0, 1000])
		bucketDiameter.setMM(304.8)
		//LengthParameter bucketDistFromWall = new LengthParameter("Bucket Distance From Wall (mm)", 75, [0, 1000])
		//bucketDistFromWall.setMM(75)
		
		// define the parameters for the monorail track shelf
		def trackStraightBezierPieces = 6
		def trackCurveBezierPieces = 12
		LengthParameter railElevation = new LengthParameter("Rail Elevation (mm)", 600, [0, 1000])
		railElevation.setMM(600)
		LengthParameter trackDistFromWall = new LengthParameter("Track Distance from Wall (mm)", 25, [0, 1000])
		trackDistFromWall.setMM(40)
		
		// define the parameters for the monorail linear gears
		LengthParameter turningRadius = new LengthParameter("Minimum Turning Radius of the Monorail (mm)", 50, [0, 300])
		turningRadius.setMM(50)
		
		// define the parameters for the construction screw holes
		LengthParameter screwDiameter = new LengthParameter("Screw Hole Diameter (mm)", 3, [0, 20])
//		screwDiameter.setMM(3)															// construction correct
		screwDiameter.setMM(10)															// temporary, for visualization
		LengthParameter screwSpacing = new LengthParameter("Distance Between Construction Screws (mm)", 150, [0, 400])
		screwSpacing.setMM(150)
		
		
		
		def kinematics = arg0.getAllDHChains().get(0)
		ICadGenerator lift = ScriptingEngine.gitScriptRun('https://github.com/TechnocopiaPlant/ForkyRobot.git', 'ForkyLiftCad.groovy')
		ArrayList<CSG> liftImportCAD = getBaseLinkCAD(kinematics)
		ArrayList<CSG> liftCAD = []
		def widthX = 0
		for (CSG c : liftImportCAD) {
			String name = c.getName()
			if (name != null && !name.toLowerCase().contains("bucket")) {
				liftCAD.add(c)
			}
			if (name != null && !name.toLowerCase().contains("board")) {
				widthX = Math.max(c.getTotalX(),widthX)
				println("widthX: ${widthX}")
			}
		}
		println("widthX: ${widthX}")
		
		CSG plantGuide = new Cube(bayDepth.getMM()/2, bayWidth.getMM(), boardThickness.getMM()).toCSG()
			.movex(bayDepth.getMM()/4)
			.movey(0)
			.movez(boardThickness.getMM()/2)
			.rotz(90)
		CSG bucketGhost = new Cylinder(bucketDiameter.getMM()/2,boardThickness.getMM(), (int) 40).toCSG()
		plantGuide = plantGuide.difference(bucketGhost)
		
		// Define the port & starboard plantShelf arms, used to guide the bucket into place
		def armDepth = bayDepth.getMM()/3
		def armWidth = (bayWidth.getMM()-bucketDiameter.getMM()) / 2
		
		// Define a possible rectangular arm geometry
		CSG armRect = new Cube(armWidth, armDepth, boardThickness.getMM()).toCSG() // rectangle is temporary
			.movex(bucketDiameter.getMM()/2+armWidth/2)
			.movey(armDepth/2)
			.movez(boardThickness.getMM()/2)
			
		// Define a possible arm geometry along a bezier curve
		BezierEditor armBezierEditor = new BezierEditor(ScriptingEngine.fileFromGit(URL, "armBez.json"),armBezierPieces)
		armBezierEditor.setStart(bucketDiameter.getMM()/2, 0, 0)
		armBezierEditor.setEnd(bayWidth.getMM()/2, armDepth, 0)
		//armBezierEditor.setCP1(bucketDiameter.getMM()/2-10, armDepth, 0)			//Used to reset control point before manually tweaking - JMS, Feb 2023
		//armBezierEditor.setCP2(bucketDiameter.getMM()/2, armDepth+10, 0)			//Used to reset control point before manually tweaking - JMS, Feb 2023
		ArrayList<Transform> armTrans = armBezierEditor.transforms()
		ArrayList<CSG> armManips = armBezierEditor.getCSG()
		List<Vector3d> armPoly = [new Vector3d(armRect.getMaxX(), armRect.getMinY(),0), new Vector3d(armRect.getMinX(),0,0)]
		for(Transform trans : armTrans) {
			armPoly.add(new Vector3d(trans.getX(),trans.getY(),trans.getZ()))
		}
		CSG armBez = Extrude.points(new Vector3d(0,0,boardThickness.getMM()),armPoly)
		
		// Use either the rectangular arms or the bezier guided arms
		CSG armGuidePort = armBez
		CSG armGuideStarboard = armGuidePort.mirrorx()
		
		plantGuide = plantGuide.union(armGuidePort)
		plantGuide = plantGuide.union(armGuideStarboard)
		plantGuide = plantGuide.movez(railElevation.getMM()/4)
		
		// Instantiate a bucket to hold fastener CSG objects in
		ArrayList<CSG> fasteners = []
		
		// Save plantShelf to a temporary CSG so that addTabs uses the correct edge lengths
		CSG plantGuideTemp = plantGuide
		
		// Add tabs to the Y- side
		ArrayList<CSG> returned = plantGuideTemp.addTabs(new Vector3d(0, -1, 0), screwDiameter);
		plantGuide = plantGuide.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		// Add tabs to the X+ side
		returned = plantGuideTemp.addTabs(new Vector3d(1, 0, 0), screwDiameter);
		plantGuide = plantGuide.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		// Add tabs to the X- side
		returned = plantGuideTemp.addTabs(new Vector3d(-1, 0, 0), screwDiameter);
		plantGuide = plantGuide.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		CSG gridBoard = new Cube(bayWidth.getMM(),bayDepth.getMM(),boardThickness.getMM()).toCSG()
							.movez(-boardThickness.getMM()/2)
							
		println "Making hole grid"
		def nutsertGridPlate= []
		def gridUnits = 25 		// 25mm grid spacing
		def gridMaxX = Math.floor(gridBoard.getMaxX()/gridUnits)
		def gridMaxY = Math.floor(gridBoard.getMaxY()/gridUnits)
		def gridMinX = Math.ceil(gridBoard.getMinX()/gridUnits)+1
		def gridMinY = Math.ceil(gridBoard.getMinY()/gridUnits)+1
		def netmoverP= new Cylinder(5.0/2,boardThickness.getMM()).toCSG() // sized to M5 bolt
					.toZMin()
					.movez(-boardThickness.getMM())
		for(int i=gridMinY;i<(gridMaxY);i++)
			for(int j=gridMinX;j<(gridMaxX);j++){
				nutsertGridPlate.add(netmoverP.movey(gridUnits*i)
						   .movex(gridUnits*j))
		}
		if (production)
			gridBoard = gridBoard.difference(nutsertGridPlate)
		
		CSG gridBoardTemp = gridBoard
		
		// Add tabs to the Y- side
		returned = gridBoardTemp.addTabs(new Vector3d(0, -1, 0), screwDiameter);
		gridBoard = gridBoard.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));

		// Add tabs to the X+ side
		returned = gridBoardTemp.addTabs(new Vector3d(1, 0, 0), screwDiameter);
		gridBoard = gridBoard.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));

		// Add tabs to the X- side
		returned = gridBoardTemp.addTabs(new Vector3d(-1, 0, 0), screwDiameter);
		gridBoard = gridBoard.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		CSG portWall = new Cube(boardThickness.getMM(),bayDepth.getMM(),bayHeight.getMM()).toCSG()
			.movex(bayWidth.getMM()/2 + boardThickness.getMM()/2)
			.movez(bayHeight.getMM()/2-boardThickness.getMM())
		CSG starboardWall = portWall.mirrorx()
		portWall = portWall.difference(plantGuide).difference(fasteners).difference(gridBoard)
		starboardWall = starboardWall.difference(plantGuide).difference(fasteners).difference(gridBoard)
		
		// Save portWall to a temporary CSG so that addTabs uses the correct edge lengths
		CSG wallTemp = portWall
		
		// Add tabs to the Y- side
		returned = wallTemp.addTabs(new Vector3d(0, -1, 0), screwDiameter);
		portWall = portWall.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		// Save starboardWall to a temporary CSG so that addTabs uses the correct edge lengths
		wallTemp = starboardWall
		
		// Add tabs to the Y- side
		returned = wallTemp.addTabs(new Vector3d(0, -1, 0), screwDiameter);
		starboardWall = starboardWall.union(returned.get(0));
		fasteners.addAll(returned.subList(1, returned.size()));
		
		CSG backWall = new Cube(bayWidth.getMM()+boardThickness.getMM()*2, boardThickness.getMM(), bayHeight.getMM()).toCSG()
			.movex(0)
			.movey(-bayDepth.getMM()/2-boardThickness.getMM()/2)
			.movez(bayHeight.getMM()/2-boardThickness.getMM())
		backWall = backWall.difference(plantGuide).difference(portWall).difference(starboardWall).difference(fasteners).difference(gridBoard)
		
		TransformNR liftTrans = TransformFactory.csgToNR(new Transform().rotz(-90))
		arg0.getAllDHChains().get(0).setRobotToFiducialTransform(liftTrans)
		
		// Assign each component an assembly step, for the exploded view visualization
		gridBoard.addAssemblyStep(1, new Transform())
		plantGuide.addAssemblyStep(2, new Transform().movez(100))
//		trackShelf.addAssemblyStep(2, new Transform().movez(100))
		backWall.addAssemblyStep(3, new Transform().movey(-50))
		portWall.addAssemblyStep(3, new Transform().movex(50))
		starboardWall.addAssemblyStep(3, new Transform().movex(-50))
		
		// Add colored components to returned list, for rendering
		back.add(gridBoard.setColor(javafx.scene.paint.Color.BROWN))
		back.add(plantGuide.setColor(javafx.scene.paint.Color.MAGENTA))
//		back.add(trackShelf.setColor(javafx.scene.paint.Color.RED))
		back.add(backWall.setColor(javafx.scene.paint.Color.BLUE))
		back.add(portWall.setColor(javafx.scene.paint.Color.CYAN))
		back.add(starboardWall.setColor(javafx.scene.paint.Color.AQUAMARINE))
		back.addAll(fasteners)
		
		
		for(CSG c:back)
			c.setManipulator(arg0.getRootListener())
			
		for(DHParameterKinematics kin:arg0.getAllDHChains()) {
			CSG limbRoot =new Cube(1).toCSG()
			limbRoot.setManipulator(kin.getRootListener())
			back.add(limbRoot)
		}
		back.addAll(armManips)		//			Uncomment to show and edit the bezier arm manipulators - JMS, Feb 2023
//		back.addAll(trackManips)	//			Uncomment to show and edit the bezier track manipulators - JMS, Mar 2023
		
		return back;
	}
	
}
