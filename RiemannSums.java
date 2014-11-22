import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath; //use Graphics2D.draw(Shape) to render General Path user function
import javax.swing.JFrame;
import java.util.ArrayList;
import java.text.DecimalFormat;

public class RiemannSums extends Applet implements MouseListener, MouseMotionListener, ItemListener, AdjustmentListener, ActionListener, Runnable{

	private Thread thread;
	private Graphics dbGraphics;
	private Image dbImage;

	private int width, height;
	private final int SCALE = 30; //30 pixels = 1 x or y value

	private Choice chooseShape, chooseMethod, chooseFunction;
	private String actualArea, calculatedArea;
	private CheckboxGroup graphOptions;
	private Checkbox[] graphOptionsBoxes;
	private Scrollbar lowerBoundScroll, upperBoundScroll, aScroll, bScroll;
	private Button goButton;

	private GeneralPath userPath;

	private boolean redrawShape, drawingUserPath;

	private double a, pow, b;

	private Polygon functionPolygon;
	private ArrayList<RiemannShape> shapes;
	private double intervalLength;
	private int lastGoodSize;

	private Font titleFont, subtitleFont, midFont, graphFont;
	private DecimalFormat df;

	public void init(){

		width = 1000;
		height = 700;
		setSize(width, height);
		setBackground(new Color(173,216,230));
		setAWTComponents();
		addMouseListener(this);
		addMouseMotionListener(this);

		redrawShape = false;
		drawingUserPath = false;

		a = 1;
		pow = 0;
		b = 0;

		shapes = new ArrayList<RiemannShape>();

		titleFont = new Font("Harrington", Font.BOLD, 36);
		subtitleFont = new Font("Papyrus", Font.BOLD, 13);
		midFont = new Font("Papyrus", Font.BOLD, 20);
		df = new DecimalFormat("###.000");
	}

	public void paint(Graphics g){

		setAWTComponentBounds();

		g.setColor(new Color(60,179,113));

		double cArea = 0;

		Graphics2D g2d = (Graphics2D)(g);

		for(int i=0; i<shapes.size(); i++){ //draw RiemannShapes

			g.fillPolygon(shapes.get(i));
			cArea += shapes.get(i).getArea();
		}

		if(lowerBoundScroll.getValue() > upperBoundScroll.getValue()) cArea *= -1;
		calculatedArea = df.format(cArea / (SCALE*SCALE));
		if(graphOptionsBoxes[0].getState()) actualArea = df.format( integrate(lowerBoundScroll.getValue()/100.0, upperBoundScroll.getValue()/100.0) );
		else actualArea = "N / A";

		g.setColor(Color.BLACK);
		drawStrings(g);

		if(graphOptionsBoxes[0].getState()){

			chooseFunction.setVisible(true);
			aScroll.setVisible(true);
			int index = chooseFunction.getSelectedIndex();
			if(index == 0) bScroll.setVisible(false);
			else bScroll.setVisible(true);


			if(index == 0) pow = 0;
			if(index == 1) pow = -1;
			if(index == 2) pow = .5;
			if(index == 3) pow = 1;
			if(index == 4) pow = 2;
			if(index == 5) pow = 3;

			a = aScroll.getValue()/-100.0;
			if(bScroll.isVisible()) b = bScroll.getValue()/-100.0;
			else b = 0;

			drawFunction(a, pow, b, g);
		}

		else{

			chooseFunction.setVisible(false);
			aScroll.setVisible(false);
			bScroll.setVisible(false);
		}

		if(userPath != null){

			Graphics2D g2 = (Graphics2D)(g);
			g2.draw(userPath);

			g.setColor(getBackground()); //pretty path
			g.fillRect(0,0,20,height);
		}
	}

	public void update(Graphics g){

		if(dbImage == null){

			dbImage = createImage(width, height);
			dbGraphics = dbImage.getGraphics();
		}

		dbGraphics.setColor(getBackground());
		dbGraphics.fillRect(0, 0, width, height);
		dbGraphics.setColor(getForeground());
		paint(dbGraphics);

		g.drawImage(dbImage, 0, 0, this);
	}

	private void drawFunction(double a, double pow, double b, Graphics g){

		functionPolygon = new Polygon();

		int start = 0;
		if(pow > 0 && pow < 1) start = 300;

		for(int x=start; x<=600; x++){ //works for y=ax^n + b only

			double xValue = -10 + ( (double)(x) / (double)(SCALE) );
			int y = screenY( a*Math.pow(xValue, pow)*SCALE + (b*SCALE) + 330); //330 = 700-xaxis (370)
			functionPolygon.addPoint(x+20, y); //graph starts at x=20, not 0
		}

		g.drawPolyline(functionPolygon.xpoints, functionPolygon.ypoints, functionPolygon.npoints); //draw open polygon
	}

	private int screenY(double y){

		return (int)(height - y);
	}

	private void addRiemannShape(){ //make your own shape!

		int shapeIndex = chooseShape.getSelectedIndex();
		int method = chooseMethod.getSelectedIndex();
		int npoints = -1;

		if(shapeIndex == 0) npoints = 3;
		if(shapeIndex == 1 || shapeIndex == 2) npoints = 4;
		if(shapeIndex == 3) npoints = 1000; //increase?

		int newSize = shapes.size();

		do{
			if(newSize > 1000){

				newSize = lastGoodSize;
				return;
			}
			newSize++;
			intervalLength = ( Math.abs( upperBoundScroll.getValue()/100.0 - lowerBoundScroll.getValue()/100.0 )*SCALE ) / ( (double)(newSize) );

			if(newSize == 1) break;

		}while( ((intervalLength*newSize) % newSize != 0));

		lastGoodSize = newSize;

		shapes.clear();

		int[][] newxpoints = new int[newSize][npoints];
		int[][] newypoints = new int[newSize][npoints];

		//works for triangles, rectangles, trapezoids, only

		for(int i=0; i<newSize; i++){

			newxpoints[i][0] = (int)( ( Math.min(lowerBoundScroll.getValue(), upperBoundScroll.getValue()) / 100.0 )*SCALE + 320 ) + (int)(i*intervalLength);
			newypoints[i][0] = 370;

			newxpoints[i][1] = (int)( newxpoints[i][0] + intervalLength );
			newypoints[i][1] = 370;

			if(shapeIndex == 0){ //triangles

				switch(method){

					case 0: newxpoints[i][2] = newxpoints[i][0]; break; //left
					case 1: newxpoints[i][2] = newxpoints[i][1]; break; //right
					case 2: newxpoints[i][2] = (int)( Math.min(newxpoints[i][0], newxpoints[i][1]) + (intervalLength / 2.0) ); break; //mid
				}

				newypoints[i][2] = getPolygonY( newxpoints[i][method] );

				RiemannShape r = new RiemannShape(newxpoints[i], newypoints[i], npoints, shapes);
			}

			if(shapeIndex == 1 || shapeIndex == 2){ //rects / traps

				newxpoints[i][2] = newxpoints[i][1];
				newxpoints[i][3] = newxpoints[i][0];


				if(shapeIndex == 1){ //rect

					int xCheck = -1;

					switch(method){

						case 0: xCheck = newxpoints[i][0]; break; //left
						case 1: xCheck = newxpoints[i][1]; break; //right
						case 2: xCheck = (int)( Math.min(newxpoints[i][0], newxpoints[i][1]) + (intervalLength / 2.0) ); break; //mid
					}

					newypoints[i][2] = getPolygonY( xCheck );
					newypoints[i][3] = newypoints[i][2];
				}

				if(shapeIndex == 2){ //trap

					newypoints[i][2] = getPolygonY( newxpoints[i][2] );
					newypoints[i][3] = getPolygonY( newxpoints[i][3] );
				}

				RiemannShape r = new RiemannShape(newxpoints[i], newypoints[i], npoints, shapes);
			}
			if(shapeIndex == 3){

				int h = (int)( newxpoints[i][0] + (intervalLength/2.0) );

				int xCheck = -1;

				switch(method){

					case 0: xCheck = newxpoints[i][0]; break; //left
					case 1: xCheck = newxpoints[i][1]; break; //right
					case 2: xCheck = (int)( Math.min(newxpoints[i][0], newxpoints[i][1]) + (intervalLength / 2.0) ); break; //mid
				}

				int k = (int)( 330 - (.5 * getPolygonY( xCheck )) );
				//center @ (h,k)
			}
		}
	}

	private int getPolygonY(int x){

		int[] allXs = functionPolygon.xpoints;
		int[] allYs = functionPolygon.ypoints;
		int length = functionPolygon.npoints;

		for(int i=0; i<length; i++){

			if(allXs[i] == x){

				return allYs[i];
			}
		}

		return 1; //should never happen
	}

	private double integrate(double c, double d){ //ex 3, 5 //math.abs - split into two integrals, with bounds being x-int, negative of negative one plus positive of postivie one

		int functionIndex = chooseFunction.getSelectedIndex();

		switch(functionIndex){

			case 0: return ( (a*d) - (a*c) );
			case 1: return ( ( a*Math.log(d) + (b*d) ) - ( a*Math.log(c) + (b*c) ) );
			case 2: return ( ( ((2.0/3.0)*a*Math.pow(d, 1.5)) + (b*d) ) - ( ((2.0/3.0)*a*Math.pow(c, 1.5)) + (b*c) ) );
			case 3: return ( ( (a/2.0)*(d*d) + (b*d) ) - ( (a/2.0)*(c*c) + (b*c) ) );
			case 4: return ( ( (a/3.0)*(d*d*d) + (b*d) ) - ( (a/3.0)*(c*c*c) + (b*c) ) );
			case 5: return ( ( (a/4.0)*(d*d*d*d) + (b*d) ) - ( (a/4.0)*(c*c*c*c) + (b*c) ) );
		}

		return 0;
	}

	public void itemStateChanged(ItemEvent e){

		Object source = e.getSource();
		shapes.clear();
		calculatedArea = null;
		actualArea = null;
		redrawShape = false;

		if(source == graphOptionsBoxes[1]){

			userPath = new GeneralPath();
			drawingUserPath = false;
		}

		if(source == graphOptionsBoxes[0]){

			drawingUserPath = false;
			userPath = null;
		}
	}

	public void adjustmentValueChanged(AdjustmentEvent e){

		Object source = e.getSource();

		if(source == lowerBoundScroll || source == upperBoundScroll){

			if(redrawShape){

				shapes.clear();
				addRiemannShape();
			}
		}
	}

	public void mouseEntered(MouseEvent e){
	}

	public void mouseExited(MouseEvent e){
	}

	public void mousePressed(MouseEvent e){

		int pressX = e.getX();
		int pressY = e.getY();

		if(graphOptionsBoxes[1].getState() && pressX <=620 && pressY >= 70 && pressY <= 670 && shapes.size() == 0){

			userPath = new GeneralPath();
			userPath.moveTo(width, height); //to create closed polygon
			userPath.lineTo(0, height);
			drawingUserPath = true;

			userPath.lineTo(pressX, pressY);
		}
	}

	public void mouseReleased(MouseEvent e){

		int releaseX = e.getX();
		int releaseY = e.getY();

		if(drawingUserPath){

			drawingUserPath = false;

			//convert GeneralPath to functionPolygon
			functionPolygon = new Polygon();

			for(int x=20; x<=620; x++){

				for(int y=0; y<=height; y++){

					if(userPath.contains(x, y)){

						functionPolygon.addPoint(x, y);
						break;
					}
				}
			}
		}
	}

	public void mouseClicked(MouseEvent e){

		int clickX = e.getX();
		int clickY = e.getY();

		for(int i=0; i<shapes.size(); i++){

			if( shapes.get(i).contains(clickX, clickY) ){

				addRiemannShape();
				break;
			}
		}
	}

	public void mouseMoved(MouseEvent e){
	}

	public void mouseDragged(MouseEvent e){

		int dragX = e.getX();
		int dragY = e.getY();

		if(graphOptionsBoxes[1].getState() && shapes.size() == 0 && drawingUserPath){

			if(userPath.getCurrentPoint().getX() < dragX) userPath.lineTo(dragX, dragY);
		}
	}

	public void actionPerformed(ActionEvent e){

		Object source = e.getSource();

		if(source == goButton){

			shapes.clear();
			addRiemannShape();
			redrawShape = true;
		}
	}

	private void drawStrings(Graphics g){

		g.setFont(titleFont);
		g.drawString("Riemann Sum Approximator",260,35);
		g.setFont(subtitleFont);
		g.drawString("Shape",745,120);
		g.drawString("Approximation Method",820,120);
		if(chooseShape.getSelectedIndex() == 2) g.drawString("(Inconsequential)",830,100);

		if(aScroll.isVisible()) g.drawString("a = " + (aScroll.getValue()/-100.0),877,210);
		if(bScroll.isVisible()) g.drawString("b = " + (bScroll.getValue()/-100.0),935,210);

		if(graphOptionsBoxes[1].getState()){

			g.drawString("Click and drag on the graph area", 740,280);
			g.drawString("to draw your function",780,300);
		}

		if(shapes.size() > 0){

			g.drawString("Click on any shape to increase the",740,510);
			g.drawString("number of Riemann shapes",770,530);
			g.drawString("approximating the curve.",780,550);
		}

		g.drawString("Bounds", 810, 335);
		g.drawString("Lower Bound", 740, 370);
		g.drawString(lowerBoundScroll.getValue()/100.0 + "", 950, 368);
		g.drawString("Upper Bound", 740, 410);
		g.drawString(upperBoundScroll.getValue()/100.0 + "", 950, 407);

		if(calculatedArea != null){

			g.setFont(midFont);
			g.drawString("Estimated Value: " + calculatedArea, 745,610);
			g.drawString("Actual Value: " + actualArea, 765,650);
		}

		g.setFont(subtitleFont);
		g.drawLine(20,370,620,370); //x axis
		for(int i=20; i<=620; i+=SCALE) g.drawLine(i, 365, i, 375);
		g.drawString("X",625,365);
		g.drawString("10",612, 385);
		g.drawLine(320,70,320,670); //y axis
		for(int i=70; i<=670; i+=SCALE) g.drawLine(315, i, 325, i);
		g.drawString("Y",325,70);
		g.drawString("10",295,75);
	}

	private void setAWTComponents(){

		chooseShape = new Choice();
		chooseShape.setBackground(new Color(60,179,113));
		chooseShape.add("Triangle");
		chooseShape.add("Rectangle");
		chooseShape.add("Trapezoid");
		chooseShape.setFocusable(false);
		chooseShape.addItemListener(this);
		add(chooseShape);

		chooseMethod = new Choice();
		chooseMethod.setBackground(new Color(144,238,144));
		chooseMethod.add("Left Sum");
		chooseMethod.add("Right Sum");
		chooseMethod.add("Middle Sum");
		chooseMethod.setFocusable(false);
		chooseMethod.addItemListener(this);
		add(chooseMethod);

		chooseFunction = new Choice();
		chooseFunction.setBackground(Color.LIGHT_GRAY);
		chooseFunction.add("f(x)=a");
		chooseFunction.add("f(x)=ax^-1 + b");
		chooseFunction.add("f(x)=ax^.5 + b");
		chooseFunction.add("f(x)=ax + b");
		chooseFunction.add("f(x)=ax^2 + b");
		chooseFunction.add("f(x)=ax^3 + b");
		chooseFunction.select(3);
		chooseFunction.setFocusable(false);
		chooseFunction.addItemListener(this);
		chooseFunction.setVisible(false);
		add(chooseFunction);

		lowerBoundScroll = new Scrollbar(Scrollbar.HORIZONTAL, -1000, 10, -1000, 1010);
		lowerBoundScroll.setBackground(new Color(219,112,147));
		lowerBoundScroll.setFocusable(false);
		lowerBoundScroll.addAdjustmentListener(this);
		add(lowerBoundScroll);

		upperBoundScroll = new Scrollbar(Scrollbar.HORIZONTAL, 1000, 10, -1000, 1010);
		upperBoundScroll.setBackground(new Color(219,112,147));
		upperBoundScroll.setFocusable(false);
		upperBoundScroll.addAdjustmentListener(this);
		add(upperBoundScroll);

		aScroll = new Scrollbar(Scrollbar.VERTICAL, -100, 5, -1000, 1005); //switched for some reason - must negatize
		aScroll.setBackground(Color.LIGHT_GRAY);
		aScroll.setFocusable(false);
		aScroll.addAdjustmentListener(this);
		add(aScroll);

		bScroll = new Scrollbar(Scrollbar.VERTICAL, 0, 5, -1000, 1005); //switched for some reason - must negatize
		bScroll.setBackground(Color.LIGHT_GRAY);
		bScroll.setFocusable(false);
		bScroll.addAdjustmentListener(this);
		add(bScroll);

		goButton = new Button("ESTIMATE");
		goButton.setFocusable(false);
		goButton.addActionListener(this);
		add(goButton);

		graphOptions = new CheckboxGroup();
		graphOptionsBoxes = new Checkbox[2];
		graphOptionsBoxes[0] = new Checkbox("Select Function", graphOptions, true);
		graphOptionsBoxes[1] = new Checkbox("Draw Function", graphOptions, false);
		for(Checkbox c : graphOptionsBoxes){

			c.setFocusable(false);
			c.addItemListener(this);
			add(c);
		}
	}

	private void setAWTComponentBounds(){

		chooseMethod.setBounds(860, 130, 93, 21);
		chooseShape.setBounds(730, 130, 84, 21);
		for(int i=0; i<2; i++) graphOptionsBoxes[i].setBounds(765, 190 + 30*i, 100,23);
		chooseFunction.setBounds(775,260,84,21);
		aScroll.setBounds(895, 220, 22, 100);
		bScroll.setBounds(945, 220, 22, 100);
		lowerBoundScroll.setBounds(840, 356, 100, 22);
		upperBoundScroll.setBounds(845, 395, 100, 22);
		goButton.setBounds(790,450,100,30);
	}

	public void start(){

		if(thread == null){

			thread = new Thread(this);
			thread.start();
		}
	}

	public void run(){

		while(thread != null){

			repaint();

			try{

				Thread.sleep(20);
			}
			catch(InterruptedException e){
			}
		}
	}

	public void stop(){

		thread = null;
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Riemann Sum Approximator");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.getContentPane().setBackground(Color.WHITE);
		Applet thisApplet = new RiemannSums();
		frame.getContentPane().add(thisApplet, BorderLayout.CENTER);
		thisApplet.init();
		frame.setSize(thisApplet.getSize());
		thisApplet.start();
		frame.setVisible(true);
	}
}
