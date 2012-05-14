package org.es.uremote.computer;

import static org.es.uremote.utils.Constants.DEBUG;

import java.util.ArrayList;
import java.util.List;

import org.es.uremote.R;
import org.es.uremote.components.FileManagerAdapter;
import org.es.uremote.network.AsyncMessageMgr;
import org.es.uremote.objects.FileManagerEntity;
import org.es.uremote.utils.IntentKeys;
import org.es.uremote.utils.Message;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FragFileManager extends ListFragment {
	private static final String TAG = "FileManager";

	private TextView mTvPath; 
	private String mDirectoryPath;
	private String mDirectoryContent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO recup�rer le repertoire courant ici (getExtra)
		mDirectoryPath		= getActivity().getIntent().getStringExtra(IntentKeys.DIR_PATH);
		mDirectoryContent	= getActivity().getIntent().getStringExtra(IntentKeys.DIR_CONTENT);

		if (mDirectoryPath == null || mDirectoryContent == null) {
			mDirectoryPath		= "L:\\Series";
			mDirectoryContent = "..<DIR>|24<DIR>|Breaking Bad<DIR>|Dexter<DIR>|Futurama<DIR>|Game of Thrones<DIR>|Glee<DIR>|Heroes<DIR>|House<DIR>|How I Met Your Mother<DIR>|Legend of the Seeker<DIR>|Merlin<DIR>|Misfits<DIR>|No Ordinary Family<DIR>|Prison Break<DIR>|Scrubs<DIR>|Smallville<DIR>|South Park<DIR>|Terminator The Sarah Connor Chronicles<DIR>|The Vampire Diaries<DIR>|The Walking Dead<DIR>|Thumbs.db<4608 bytes>";
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.filemanager, container, false);
		mTvPath = (TextView) view.findViewById(R.id.tvPath);
		return view;
	}

	@Override
	public void onStart() {
		updateView(mDirectoryContent);
		super.onStart();
	}

	/**
	 * Transforme le message s�r�alis� en liste de fichiers pour l'arborescence
	 * @param dirInfo Les informations sur le dossier s�r�alir�es
	 * @return La liste des FileManagerEntity pr�ts � �tre affich�s
	 */
	private List<FileManagerEntity> directoryInfoToList(String dirInfo) {
		if (dirInfo == null || dirInfo.isEmpty())
			return null;

		final List<FileManagerEntity> fileList = new ArrayList<FileManagerEntity>();
		String[] filesInfo = dirInfo.split("[|]");
		for (String fileInfo : filesInfo) {
			fileList.add(new FileManagerEntity(mDirectoryPath, fileInfo));
		}
		return fileList;
	}

	/**
	 * Fonction permettant de mettre � jour de la vue
	 * @param infos Les �l�ments constitutifs du dossier � afficher (donn�es s�r�alis�es)
	 */
	private void updateView(String infos){
		final List<FileManagerEntity> fileList = directoryInfoToList(infos);

		if (fileList.size() == 0) {
			if (DEBUG)
				Log.e(TAG, "fileList is null");
			return;
		}

		FileManagerAdapter adpt = new FileManagerAdapter(getActivity().getApplicationContext(), fileList);
		setListAdapter(adpt);

		ListView listView = getListView();
		listView.setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _parent, View _view, int _position, long _id) {
				if (fileList.get(_position).isDirectory()) {
					getDirectoryContent(fileList.get(_position).getFullPath());

				} else {
					// Ouvrir le fichier avec le programme par d�faut
					openFile(fileList.get(_position).getFullPath());

				}
			}
		});

		int pathLength = mDirectoryPath.length();
		// On n'affiche que les derniers caract�res
		//TODO G�rer l'orientation
		String path = (pathLength > 33) ? "..." + mDirectoryPath.substring(pathLength - 30, pathLength) : mDirectoryPath;
		mTvPath.setText(path);
	}

	/**
	 * Demande au serveur de lister le contenu du r�pertoire.
	 * Lance l'activit� une fois les donn�es r�cup�r�e.
	 * @param _dirPath Le chemin du r�pertoire � afficher. 
	 */
	private void getDirectoryContent(String _dirPath) {
		sendAsyncMessage(Message.OPEN_DIR, _dirPath);
	}

	private void openDirectory(String _dirPath, String _dirContent) {
		// Afficher le contenu du r�pertoire
		mDirectoryPath = _dirPath;
		mDirectoryContent = _dirContent;
		updateView(mDirectoryContent);
	}

	private void openFile(String _filename) {
		sendAsyncMessage(Message.OPEN_FILE, _filename);
	}

	/**
	 * Cette fonction initialise le composant g�rant l'envoi des messages 
	 * puis envoie le message pass� en param�tre.
	 * @param _code Le code du message. 
	 * @param _param Le param�tre du message.
	 */
	private void sendAsyncMessage(String _code, String _param) {
		if (MessageMgr.availablePermits() > 0) {
			new MessageMgr().execute(_code, _param);
		} else {
			Toast.makeText(getActivity().getApplicationContext(), "No more permit available !", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Classe asynchrone de gestion d'envoi des messages au serveur
	 * @author cyril.leroux
	 */
	public class MessageMgr extends AsyncMessageMgr {

		@Override
		protected void onPostExecute(String _serverReply) {
			super.onPostExecute(_serverReply);

			if (Message.RC_ERROR.equals(mReturnCode)) {
				Toast.makeText(getActivity().getApplicationContext(), _serverReply, Toast.LENGTH_SHORT).show();
			} else if (Message.OPEN_DIR.equals(mCommand)) {
				openDirectory(mParam, _serverReply);
			} 
		}
	}
}
