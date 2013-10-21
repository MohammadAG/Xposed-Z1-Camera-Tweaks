package com.mohammadag.z1cameratweaks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.pref_general);

		OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				sendBroadcast(new Intent(Constants.INTENT_SETTINGS_UPDATED));
				return true;
			}
		};

		findPreference(Constants.KEY_DISABLE_FASTCAPTURE_SOUND).setOnPreferenceChangeListener(listener);
		findPreference(Constants.KEY_DISABLE_HW_BURST).setOnPreferenceChangeListener(listener);
		findPreference("copyright_key").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse("http://mohammadag.xceleo.org/redirects/z1_camera_tweaks.html"));
				startActivity(i);
				return false;
			}
		});
		
		findPreference("donate_key").setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=RXKU7ZY8Q3SVE"));
				startActivity(i);
				return false;
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
