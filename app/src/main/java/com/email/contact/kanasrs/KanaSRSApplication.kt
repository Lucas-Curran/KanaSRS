package com.email.contact.kanasrs

import android.app.Application
import android.content.Context
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

class KanaSRSApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initAcra {
            //core configuration:
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST
            mailSender {
                //required
                mailTo = "report.jwriter@gmail.com"
                //defaults to true
                reportAsFile = true
                //defaults to ACRA-report.stacktrace
                reportFileName = "Crash.txt"
                //defaults to "<applicationId> Crash Report"
                subject = "JWriter Crash Report"
                //defaults to empty
                //body = getString(R.string.mail_body)
            }
            //each plugin you chose above can be configured in a block like this:
            dialog {
                //required
                text = "Sorry the application has crashed! You can send a report to the developers."
                //optional, enables the dialog title
                title = "Crash"
                //optional, enables the comment input
                commentPrompt = "Add a comment on when the app crashed:"
                resTheme = R.style.DialogTheme
            }
        }
    }
}