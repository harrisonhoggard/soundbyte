package bot;

import events.util.TimedEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

// A customized calendar system thread. This allows me to use time-based events.
public class Calendar implements Runnable{

	// Initializes the calendar by running the thread.
	public Calendar() {
		Bot.log(getLogType(), "Initializing calendar");

		Thread thread = new Thread(this);
		thread.start();
	}

	// Retrieves the refresh time. Refreshes at every new hour.
	private int refreshUpdateTime() {
		return Integer.parseInt(Bot.getLocalTime().plusHours(1).format(DateTimeFormatter.ofPattern("hh")));
	}

	// The name that is printed in the logger.
	private static String getLogType() {
		return "CALENDAR";
	}

	// "Thread" that runs, executing any event that needs to be called at the refresh time.
	public void run() {
		do {
			try {
				waitUntil();

				// Need to keep this when there aren't events to slow it down, otherwise it executes a lot until the next refresh time.
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			TimedEvent.timedEvents.forEach((eventName, event) -> {
				if (event.getExecuteTime() == refreshUpdateTime())
				{
					event.execute(/*arguments*/);
					event.devMessage(event.getName(), event.getAction(), event.getGuild());
				}
			});

			if (Bot.getLocalTime().getHour() == LocalTime.MIDNIGHT.getHour())
			{
				Bot.writeLog();
			}

		} while (true);
	}

	// Calculates the amount of time to sleep, and waits that much time until ready to execute events again.
	private void waitUntil() throws InterruptedException {
		LocalTime refreshTime;
		if (refreshUpdateTime() < 10)
			refreshTime = LocalTime.parse("0" + refreshUpdateTime() + ":00:00");
		else
			refreshTime = LocalTime.parse(refreshUpdateTime() + ":00:00");
		Bot.log(getLogType(), "Refreshing at " + refreshTime.format(DateTimeFormatter.ofPattern("hh:mm:ss")));

		int minute = refreshTime.minusMinutes(Bot.getLocalTime().getMinute() + 1).getMinute();
		int second = refreshTime.minusSeconds(Bot.getLocalTime().getSecond() + 1).getSecond();
		int nano = refreshTime.minusSeconds(Bot.getLocalTime().getNano() + 1 ).getNano();

		TimeUnit.MINUTES.sleep(minute);
		TimeUnit.SECONDS.sleep(second);
		TimeUnit.NANOSECONDS.sleep(nano);
	}
}
