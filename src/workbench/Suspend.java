package workbench;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Suspend 
	implements Runnable
{
  private JTextField t = new JTextField(10);
  private JButton suspend = new JButton("Suspend");
  private JButton resume = new JButton("Resume");
  //private Suspendable sp = new Suspendable();
  
  //class Suspendable extends Thread {
  
    private int count = 0;
    private boolean suspended = true;
    //public Suspendable() 
    //{ 
    //   this.setDaemon(true);
    //   start(); 
    //}
    
    public void suspendThread() { 
      suspended = true;
    }
    public synchronized void resumeThread() {
      suspended = false;
      notify();
    }
    
    public void run() 
    {
      while (true) 
      {
        try 
        {
          if (suspended) 
          {
              synchronized(this) {
                while(suspended)
                  wait();
              }
          }
        } 
        catch(InterruptedException e) 
        {
          System.err.println("Interrupted");
        }
        for (int i = 0; i < 100; i++)
        {
            t.setText(Integer.toString(count++));
        }
        suspendThread();
      }
    }
  //} 
  
  public void init() {
    this.suspendThread();
    Thread th = new Thread(this);
    th.setDaemon(true);
    th.start();
    
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    f.addWindowListener(
              new java.awt.event.WindowAdapter()
              {
                public void windowClosing(java.awt.event.WindowEvent evt)
                {
                  System.exit(0);
                }
              }
    );

    Container cp = f.getContentPane();
    cp.setLayout(new FlowLayout());
    cp.add(t);
    suspend.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          suspendThread();
        }
      });
    cp.add(suspend);
    resume.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          resumeThread();
        }
      });
    cp.add(resume);
    f.pack();
    //f.setSize(500,500);
    f.show();
  }
  
  public static void main(String[] args) 
  {
    Suspend s = new Suspend();
    s.init();
  }
}


