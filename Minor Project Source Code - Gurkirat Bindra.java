import org.firmata4j.I2CDevice;
import org.firmata4j.IODevice;
import org.firmata4j.Pin;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.ssd1306.MonochromeCanvas;
import org.firmata4j.ssd1306.SSD1306;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;




public class Example2 {

    static final int A0 = 14;             // defining components such as sensor, water pump etc.
    static final int A2 = 16;
    static final int D6 = 6;
    static final int D4 = 4;
    static final byte I2C0 = 0x3C;

    public static void main(String[] args)
            throws InterruptedException, IOException {

        var myPort = "COM3";  // Arduino connection port

        IODevice device = new FirmataDevice(myPort);
        device.start();
        System.out.println("Connecting");  // prompt that describes connection in progress
        device.ensureInitializationIsDone();
        System.out.println("Connected");   // prompt that confirms the connection is established

        Pin MoistureSensor = device.getPin(16);    // moisture sensor

        Pin Pump = device.getPin(2);  // water pump
        Pump.setMode(Pin.Mode.OUTPUT);

        Pin led = device.getPin(4); // LED
        led.setMode(Pin.Mode.OUTPUT);

        I2CDevice I2CDevice = device.getI2CDevice ((byte) 0x3C);  // OLED display
        SSD1306 Oled = new SSD1306 (I2CDevice, SSD1306.Size.SSD1306_128_64);
        Oled.init();


        JFrame window = new JFrame();
        window.setTitle("Minor Project");
        window.setSize(800, 600);
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


// Creating a graph that will read the moisture sensor vales and display them on the screen

        JLabel label = new JLabel();
        label.setLocation(550, 250);
        label.setFont(new Font("Sans Serif", Font.ITALIC,15));
        window.add(label, BorderLayout.EAST);


        XYSeries series = new XYSeries("Sensors Readings");
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Sensors Readings", "Time (seconds)", " Sensor Values", dataset);
        window.add(new ChartPanel(chart), BorderLayout.CENTER);

        window.setVisible(true);

        Timer timer = new Timer();
        TimerTask myTask = new Example2Task(Oled, MoistureSensor, label, Pump, series, window, led);
        timer.schedule (myTask, new Date(), 500) ;


    }
}

-----CODE below CAN BE USED IN SECOND CLASS OR Main CLASS-----
class Example2Task extends TimerTask {

    private int x = 0;

    private final SSD1306 myOled;
    private final Pin MoistureSensor;
    private final JLabel label;
    private final Pin Pump;
    private final XYSeries series;
    private final JFrame window;
    private final Pin led;

    public Example2Task(SSD1306 myOled, Pin MoistureSensor, JLabel label, Pin Pump, XYSeries series, JFrame window, Pin led) {
        this.myOled = myOled;
        this.MoistureSensor = MoistureSensor;
        this.label = label;
        this.Pump = Pump;
        this.series = series;
        this.window = window;
        this.led = led;


    }

    @Override
    public void run() {

        myOled.getCanvas().clear();

        int value = (int) MoistureSensor.getValue();
        String MoistureSensorString = String.valueOf(value);

        int Value1 = (int) Math.floor(((value)-400));
        String MoistureValue1 = String.valueOf(Value1);


        int a = (int) Math.floor(((value) / 500.0) * 40);
        int b = (int) Math.floor(((value) / 500.0 * 15));

        if (value > 443){             // If sensor reading is HIGHER than this value, START pumping water

            try {
                Pump.setValue(255);
                Thread.sleep(3000);
                led.setValue(0);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }

        else if (value < 410 ){     // If sensor reading is LOWER than this value, STOP pumping water

            try {
                Pump.setValue(0);
                Thread.sleep(3000);
                led.setValue(1);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        myOled.getCanvas().setTextsize(2);
        myOled.getCanvas().drawHorizontalLine(0, 40, a, MonochromeCanvas.Color.BRIGHT);
        myOled.getCanvas().drawCircle(102, 35, b, MonochromeCanvas.Color.BRIGHT);
        myOled.getCanvas().drawString(0, 0, "MV : " + MoistureSensorString);
        myOled.display();

        label.setText("  Moisture Sensor value is : " + MoistureSensorString + "     ");  // display the sensor reading value on the OLED screen


        Scanner scanner = new Scanner(MoistureValue1);

        while (scanner.hasNextLine()) {
            try {
                String line = scanner.nextLine();
                int number = Integer.parseInt(line);
                series.add(x++, 50 - number);
                window.repaint();
            } catch (Exception ex) {
            }
        }
        scanner.close();


    }
}

