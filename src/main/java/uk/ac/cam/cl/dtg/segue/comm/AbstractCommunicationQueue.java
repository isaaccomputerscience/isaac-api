/**
 * Copyright 2015 Alistair Stead
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.comm;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract message queue class.
 *
 * @author Alistair Stead
 *
 * @param <T>
 *            type of message to send
 */
public abstract class AbstractCommunicationQueue<T extends ICommunicationMessage> {

  /**
   * Comparator that tells the priority queue which email should be sent first.
   */
  private final Comparator<ICommunicationMessage> emailPriorityComparator = (first, second) -> {
    if (first.getPriority() == second.getPriority()) {
      return 0;
    } else if (first.getPriority() <= second.getPriority()) {
      return -1;
    } else {
      return 1;
    }
  };

  private final PriorityBlockingQueue<T> messageSenderRunnableQueue =
      new PriorityBlockingQueue<>(100, emailPriorityComparator);

  private static final Logger log = LoggerFactory.getLogger(AbstractCommunicationQueue.class);

  private final ICommunicator<T> communicator;

  private final ExecutorService executorService;


  /**
   * FIFO queue manager that sends messages.
   *
   * @param communicator
   *            A class to send messages
   */
  protected AbstractCommunicationQueue(final ICommunicator<T> communicator) {
    this.communicator = communicator;
    this.executorService = Executors.newFixedThreadPool(2);
  }

  protected void addToQueue(final T queueObject) {
    messageSenderRunnableQueue.add(queueObject);
    executorService.submit(new MessageSenderRunnable());
    log.debug("Added to the email queue. Current size: {}", messageSenderRunnableQueue.size());
  }

  /**
   * Runnable class that sends the oldest message on the queue.
   *
   * @author Alistair Stead
   *
   */
  class MessageSenderRunnable implements Runnable {
    @Override
    public void run() {
      // Send the actual message
      try {
        T queueItem = getLatestQueueItem();
        communicator.sendMessage(queueItem);
        log.info("Sent message. Current size: {}", messageSenderRunnableQueue.size());
      } catch (CommunicationException e) {
        log.warn("Communication Exception", e);
      } catch (Exception e) {
        log.warn("Generic Exception", e);
      }
    }

    private T getLatestQueueItem() {
      return messageSenderRunnableQueue.poll();
    }
  }
}
