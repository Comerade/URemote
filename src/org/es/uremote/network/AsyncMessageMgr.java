package org.es.uremote.network;

import static org.es.uremote.utils.Constants.DEBUG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.Semaphore;

import org.es.uremote.utils.Message;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Classe asynchrone de gestion d'envoi de command avec param�tres au serveur.
 * @author cyril.leroux
 */
public class AsyncMessageMgr extends AsyncTask<String, int[], String> {
	public static Semaphore sSemaphore = new Semaphore(1);
	private static final String TAG = "AsyncMessageMgr";
	private static final int PORT = 8082;
	private static final String HOST = "192.168.0.1";
	private static final int CONNECTION_TIMEOUT = 500;
	
// TODO
	private Socket mSocket		= null;
	protected String mCommand;
	protected String mParam;
//	private OutputStream mOut	= null;
//	private InputStream mIn		= null;
//	private String mReply		= null;

	/**
	 * Cette fonction est ex�cut�e avant l'appel � {@link #doInBackground(String...)} 
	 * Ex�cut�e dans le thread principal.
	 */
	@Override
	protected void onPreExecute() {
		try {
			sSemaphore.acquire();
		} catch (InterruptedException e) {
			if (DEBUG)
				Log.e(TAG, "onPreExecute Semaphore acquire error.");
		}
		if (DEBUG)
			Log.i(TAG, "onPreExecute Semaphore acquire. " + sSemaphore.availablePermits() + " left");
		// TODO Mise � jour de l'�tat : "Connexion en cours" 
	}

	/**
	 * Cette fonction est ex�cut�e sur un thread diff�rent du thread principal
	 * @param _params le tableau de string contenant les param�tres (commande et param�tre de la commande).
	 * @return La r�ponse du serveur.
	 */
	@Override
	protected String doInBackground(String... _params) {
		String serverReply = "";

		mCommand	= _params[0];
		mParam 		= (_params.length > 1) ? _params[1] : "";
		final String message	= mCommand + "|" + mParam;

		mSocket = null;
		try {
			
			// Cr�ation du socket
			mSocket = connectToRemoteSocket(HOST, PORT, message);
			if (mSocket != null && mSocket.isConnected())
				serverReply = sendAsyncMessage(mSocket, message);
			
		} catch (IOException e) {
			mCommand = Message.ERROR;
			serverReply = "IOException" + e.getMessage();
			if (DEBUG)
				Log.e(TAG, serverReply);

		} catch (Exception e) {
			mCommand = Message.ERROR;
			serverReply = "IOException" + e.getMessage();
			if (DEBUG) 
				Log.e(TAG, serverReply);

		} finally {
			closeSocketIO();
		}

		return serverReply;
	}

	/**
	 * Cette fonction est ex�cut�e apr�s l'appel � {@link #doInBackground(String...)} 
	 * Ex�cut�e dans le thread principal.
	 * @param _serverReply La r�ponse du serveur renvoy�e par la fonction {@link #doInBackground(String...)}.
	 */
	@Override
	protected void onPostExecute(String _serverReply) {

		sSemaphore.release();
		if (DEBUG)
			Log.i(TAG, "Semaphore release");
		
//		if (_serverReply != null && !_serverReply.isEmpty()) {
//						
//			if (DEBUG)
//				Log.i(TAG, "Get a reply : " + _serverReply);
//
//			// TODO notifier que la commande a �t� re�ue
//			//updateConnectionStateOK();
//			
//			// TODO Traitement du message de retour
//			if (_serverReply.equals(Message.REPLY_VOLUME_MUTED)) {
//				// TODO Update mute image
//				//((ImageButton) findViewById(R.id.btnCmdMute)).setImageResource(R.drawable.volume_muted);
//			} else if (_serverReply.equals(Message.REPLY_VOLUME_ON)) {
//				// TODO Update mute image
//				//((ImageButton) findViewById(R.id.btnCmdMute)).setImageResource(R.drawable.volume_on);
//			} else if (Message.OPEN_DIR.equals(mCommand)) {
//				//openDirectory(mParam, _serverReply);
//
//			} else if (Message.ERROR.equals(mCommand)) {
//				// TODO Toast erreur
//				//Toast.makeText(getActivity().getApplicationContext(), _serverReply, Toast.LENGTH_SHORT).show();
//			}
//		} else {
//			// TODO notifier d'un probl�me lors de l'envoi
//			//updateConnectionStateKO();
//		}
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		closeSocketIO();
	}
	
	/**
	 * Fonction de connexion � un socket disant.
	 * @param _host L'adresse ip de l'h�te auquel est li� le socket.
	 * @param _port Le num�ro de port de l'h�te auquel est li� le socket.
	 * @param _message Le message � envoyer.
	 * @return true si la connexion s'est effectu�e correctement, false dans les autres cas.
	 * @throws IOException excteption
	 */
	private Socket connectToRemoteSocket(String _host, int _port, String _message) throws IOException {

		final SocketAddress socketAddress = new InetSocketAddress(_host, _port);
		Socket socket = new Socket();
		socket.connect(socketAddress, CONNECTION_TIMEOUT);

		return socket;
	}

	/**
	 * Cette fonction est appel�e depuis le thread principal
	 * Elle permet l'envoi d'une commande et d'un param�tre 
	 * @param _socket Le socket sur lequel on envoie le message.
	 * @param _message Le message � transmettre
	 * @return La r�ponse du serveur.
	 * @throws IOException exception.
	 */
	private String sendAsyncMessage(Socket _socket, String _message) throws IOException {
		if (DEBUG)
			Log.i(TAG, "sendMessage: " + _message);
		String serverReply = "";

		if (mSocket.isConnected()) {
			_socket.getOutputStream().write(_message.getBytes());
			_socket.getOutputStream().flush();
			_socket.shutdownOutput();
			serverReply = getServerReply(_socket);
		}
		return serverReply;
	}

	/**
	 * @param _socket Le socket auquel le message a �t� envoy�.
	 * @return La r�ponse du serveur.
	 * @throws IOException exeption
	 */
	private String getServerReply(Socket _socket) throws IOException {
		final int BUFSIZ = 512; 

		final BufferedReader bufferReader = new BufferedReader(new InputStreamReader(_socket.getInputStream()), BUFSIZ);
		String line = "", reply = "";
		while ((line = bufferReader.readLine()) != null)
			reply += line;

		if (DEBUG)
			Log.i(TAG, "Got a reply : " + reply);

		return reply;
	} 
	
	/**
	 * Ferme les entr�es/sortie du socket puis ferme le socket.
	 */
	private void closeSocketIO() {
		if (mSocket == null)
			return;

		try { if (mSocket.getInputStream() != null)	mSocket.getInputStream().close();	} catch(IOException e) {}
		try { if (mSocket.getOutputStream() != null)mSocket.getOutputStream().close();	} catch(IOException e) {}
		try { mSocket.close(); } catch(IOException e) {}
	}

}