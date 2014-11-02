import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class RiemannShape extends Polygon{

	private ArrayList<RiemannShape> intervals;

	public RiemannShape(){

		super();
	}

	public RiemannShape(int[] xpoints, int[] ypoints, int npoints, ArrayList<RiemannShape> a){

		super(xpoints, ypoints, npoints);
		intervals = a;
		intervals.add(this);
	}

	public double getArea(){

		//(signed) Area of a polygon = 1/2 |x1 x2| + |x2 x3| + |...| + |xn x0| = 1/2 sum of all determinants of matrices of points
		//                                 |y1 y2|   |y2 y3|   |...|   |yn y0|
		double area = 0;

		for(int i=0; i<npoints; i++){

			if(i == npoints-1) area += (xpoints[i]*ypoints[0])-(xpoints[0]*ypoints[i]);

			else area += (xpoints[i]*ypoints[i+1])-(xpoints[i+1]*ypoints[i]);
		}

		area /= -2.0;
		//area = Math.abs(area);

		return area;
	}
}



