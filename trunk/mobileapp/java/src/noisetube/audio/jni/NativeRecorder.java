package noisetube.audio.jni;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import noisetube.audio.ILeqListener;
import noisetube.audio.java.IStreamRecorder;
import noisetube.audio.java.StreamAudioListener;
import noisetube.util.Logger;

public class NativeRecorder extends AbstractSocketCom implements Runnable,
		IStreamRecorder
{

	ILeqListener listener;
	int retry;
	static final long TIMEOUT = 3000;
	Logger log = Logger.getInstance();
	long last_check;
	NativeRecorderCmd commander = new NativeRecorderCmd();
	volatile boolean running = false;
	Timer timer = new Timer();

	public NativeRecorder()
	{
		setPort(8113);
	}

	public void start()
	{
		if(!isRunning())
		{
			try
			{
				commander.sendRecordCmd();

				new Thread(this).start();

				timer.schedule(new TimerTask()
				{

					public void run()
					{
						if(!isLiving())
						{
							log.error("restarting server connection");

							stop();
							start();

						}

					}
				}, 0, 3000);
			}
			catch(Exception e)
			{
				log.error(e, "start recorder");
			}
		}
		else
		{
			log.error("error: recorder already running");
		}
	}

	public boolean isLiving()
	{
		if(last_check == 0)
		{
			last_check = System.currentTimeMillis();
			return true;
		}
		else
		{
			long time = (System.currentTimeMillis() - last_check) / 1000;
			if(time > TIMEOUT)
			{
				return false;
			}
			else
			{
				return true;
			}
		}
	}

	public boolean isRunning()
	{
		return running;
	}

	public void stop()
	{
		if(isRunning())
		{
			running = false;
			timer.cancel();
			try
			{
				commander.sendStopCmd();
				close();
			}
			catch(Exception e)
			{
				log.error(e, "stop recorder");
			}
		}
	}

	public void run()
	{

		int i = 0;
		running = true;
		String s = "";
		long start = 0, now = 0;
		byte[] buf = ("1").getBytes();

		byte data[] = new byte[7];
		try
		{
			while(running)
			{
				connect();
				/*
				 * new Thread(new Runnable(){ public void run(){
				 * Thread.sleep(3000); } });
				 */
				start = now;
				now = System.currentTimeMillis();
				long timeL = (now - start);

				in.read(data);
				s = new String(data);

				// response
				out.write(buf, 0, buf.length);
				out.flush();

				log.debug("latency: " + (timeL));
				log.debug("leq received: " + s);

				double val = Double.parseDouble(s);
				// -200 due to hack by adding +200 in the Native C++ send
				// function to get only
				// positive numbers
				listener.sendLeq((val - 200));
				i++;
				close();

			}
		}
		catch(IOException e1)
		{
			log.error("receive leq error: " + e1.getMessage());

			stop();

		}
		catch(NumberFormatException e)
		{
			log.error("leq error data format");
		}
	}

	public void setLeqListener(ILeqListener listener)
	{
		this.listener = listener;
	}

	public void setRecordingTime(int i)
	{

	}

	public void setStreamAudioListener(StreamAudioListener listener)
	{

	}

	public void setTimeInterval(int i)
	{

	}

}
