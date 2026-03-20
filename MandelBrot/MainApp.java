package MandelBrot;

//GUI application to display Mandelbrot set

//import statements
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.color.*;
import java.awt.geom.Rectangle2D.Double;
import java.awt.geom.Rectangle2D.Float;
import java.awt.geom.Rectangle2D;

//MainApp class
public class MainApp {
    //main method
    public static void main(String[] args) {
        //create a new MandelbrotFrame object
        MandelbrotFrame frame = new MandelbrotFrame();
        //set the frame to visible
        frame.setVisible(true);
    }
}

//MandelbrotFrame class
class MandelbrotFrame {

    public void setVisible(boolean b) {
        // Make the Mandelbrot frame visible
        //draw the Mandelbrot set
        MandelbrotPanel panel = new MandelbrotPanel();
        //create a new JFrame object
        JFrame frame = new JFrame("Mandelbrot Set");
        //set the size of the frame
        frame.setSize(800, 600);
        //set the frame to exit on close
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //add the panel to the frame
        frame.add(panel);
        //set the frame to visible
        frame.setVisible(true);
    }
}

//MandelbrotPanel class
class MandelbrotPanel extends Component{
    
        //constructor
        public MandelbrotPanel() {
            //set the size of the panel
            setPreferredSize(new Dimension(800, 600));
        }
    
        //paintComponent method
        public void paintComponent(Graphics g) {
            //create a new BufferedImage object
            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            //create a new Graphics2D object
            Graphics2D g2 = image.createGraphics();
            //create a new Rectangle2D object
            Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, 800, 600);
            //create a new GradientPaint object
            GradientPaint gradient = new GradientPaint(0, 0, Color.BLACK, 800, 600, Color.WHITE);
            //set the paint of the Graphics2D object
            g2.setPaint(gradient);
            //fill the rectangle
            g2.fill(rect);
            //create a new Mandelbrot object
            Mandelbrot mandelbrot = new Mandelbrot();
            //draw the Mandelbrot set
            mandelbrot.draw(g2);
            //draw the image
            g.drawImage(image, 0, 0, null);
        }

        //getPreferredSize method
        public Dimension getPreferredSize() {
            //return the preferred size of the panel
            return new Dimension(800, 600);
        }

}

class Mandelbrot {
    //draw method
    public void draw(Graphics2D g2) {
        //create a new Color object
        Color color = new Color(0, 0, 0);
        //set the color of the Graphics2D object
        g2.setColor(color);
        //create a new Rectangle2D object
        Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, 800, 600);
        //draw the rectangle
        g2.draw(rect);
        //create a new Color object
        Color color2 = new Color(255, 255, 255);
        //set the color of the Graphics2D object
        g2.setColor(color2);
        //create a new Rectangle2D object
        Rectangle2D.Double rect2 = new Rectangle2D.Double(0, 0, 800, 600);
        //fill the rectangle
        g2.fill(rect2);
    }
}