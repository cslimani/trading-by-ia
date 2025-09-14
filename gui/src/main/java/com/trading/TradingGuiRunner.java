package com.trading;

import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component	
public class TradingGuiRunner implements CommandLineRunner{

	@Autowired
	private GuiFrame guiLauncher;
	
	@Override
	public void run(String... args) throws Exception {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				showOnScreen(3, guiLauncher);
			}
		});
		
	}

	public static void showOnScreen(int screenIndex, JFrame frame) {
		GraphicsDevice[] screens = GraphicsEnvironment
				.getLocalGraphicsEnvironment()
				.getScreenDevices();

		if (screenIndex < 0 || screenIndex >= screens.length) {
			// fallback : centre sur l’écran par défaut
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			return;
		}

		GraphicsConfiguration gc = screens[screenIndex].getDefaultConfiguration();
		Rectangle bounds = gc.getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

		int usableX = bounds.x + insets.left;
		int usableW = bounds.width  - insets.left - insets.right;

		frame.pack();

		int x = usableX + (usableW - frame.getWidth())  / 2;
		int y = 0;
		frame.setLocationByPlatform(false); // on garde la main sur le placement
		frame.setLocation(x, y);
		frame.setVisible(true);
	}
	
	public static void showFullScreenOn(int screenIndex, JFrame frame) {
        GraphicsDevice[] devices = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices();

        if (devices.length == 0) throw new IllegalStateException("Aucun écran détecté.");
        if (screenIndex < 0 || screenIndex >= devices.length) screenIndex = 0;

        GraphicsDevice dev = devices[screenIndex];

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        frame.setUndecorated(true);
        frame.setResizable(false);

        if (dev.isFullScreenSupported()) {
            dev.setFullScreenWindow(frame); // plein écran exclusif
        } else {
            // Fallback : plein écran "borderless" sur l'écran cible
            Rectangle b = dev.getDefaultConfiguration().getBounds();
            frame.setBounds(b);
            frame.setVisible(true);
        }
    }
}
