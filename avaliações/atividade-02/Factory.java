/**
 * This creates the buffer and the producer and consumer threads.
 *
 * @author Gagne, Galvin, Silberschatz
 * Operating System Concepts with Java - Sixth Edition
 * Copyright John Wiley & Sons - 2003.
 */
public class Factory
{
	public static final String ALUNO = "Giovanna Bandeira de Castro";

	public static void main(String args[]) {
		System.out.println("=========================================");
		System.out.println(" Produtor-Consumidor (Bounded Buffer)");
		System.out.println(" Aluno: " + ALUNO);
		System.out.println(" Disciplina: Sistemas Operacionais - SO-262");
		System.out.println(" Atividade 02");
		System.out.println("=========================================");

		Buffer server = new BoundedBuffer();

      		// now create the producer and consumer threads
      		Thread producerThread = new Thread(new Producer(server));
      		Thread consumerThread = new Thread(new Consumer(server));

      		producerThread.setDaemon(true);
      		consumerThread.setDaemon(true);

      		producerThread.start();
      		consumerThread.start();

      		SleepUtilities.sleep(TEMPO_EXECUCAO);

      		System.out.println("=========================================");
      		System.out.println(" Fim da execucao - " + ALUNO);
      		System.out.println("=========================================");
	}

	private static final int TEMPO_EXECUCAO = 30;
}
