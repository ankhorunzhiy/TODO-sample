package com.sampleapp.ui.controller

import android.os.Bundle
import android.util.Log
import android.view.View
import com.android.newssample.R
import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter
import com.sampleapp.di.ScreenScope
import com.sampleapp.di.components.ActivityComponent
import com.sampleapp.domain.interactor.EventsConnectUseCase
import com.sampleapp.domain.model.Event
import com.sampleapp.domain.model.EventModel
import com.sampleapp.rx.SimpleSubscriber
import com.sampleapp.ui.view.ChatControllerView
import com.sampleapp.ui.view.ChatView
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Inject
import javax.inject.Named

@Layout(R.layout.screen_chat)
class ChatController(args: Bundle? = null) : BaseController<ChatView, ChatController.Presenter>(args), ChatView {

    @Inject
    lateinit var chatPresenter: Presenter

    @ScreenScope(ChatController::class)
    @Subcomponent(modules = arrayOf(Module::class))
    interface Component {
        fun inject(loginController: ChatController)
    }

    @dagger.Module
    class Module (val args: Bundle?){
        @Provides
        @Named("chatArgs")
        fun provideArgs(): Bundle? {
            return args
        }
    }

    override fun createPresenter(): Presenter {
        return chatPresenter
    }

    override fun injectToDagger(component: ActivityComponent) {
        component.chatComponent(Module(args)).inject(this)
    }

    override fun onViewCreated(root: View) {
        super.onViewCreated(root)
        setTitle(R.string.chat)
    }

    override fun notifyAdapter(eventModel: EventModel) {
        (view as ChatControllerView).notifyAdapter(eventModel)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        chatPresenter.init()
    }

    @ScreenScope(ChatController::class)
    class Presenter @Inject constructor(@Named("chatArgs")
                                        val args: Bundle?,
                                        val eventsConnectUseCase: EventsConnectUseCase)
        : MvpBasePresenter<ChatView>(){

        fun init() {
           view.showProgress()
           eventsConnectUseCase.execute(object : SimpleSubscriber<EventModel>() {
               override fun onNext(value: EventModel) {
                   super.onNext(value)
                   if(value.event == Event.CONNECT) view.hideProgress()
                   else processEvent(value)
               }

               override fun onError(e: Throwable?) {
                   super.onError(e)
                   view.hideProgress()
               }
           }, EventsConnectUseCase.Parameters(
                   arrayOf(Event.NEW_MESSAGE, Event.CONNECT, Event.USER_JOINED, Event.USER_LEFT, Event.TYPING, Event.STOP_TYPING)))}

        fun processEvent(eventModel: EventModel){
            view.notifyAdapter(eventModel)
        }
    }

    companion object{
        const val USER_NAME_KEY = "user_name_key"

        fun newArgs(userName: String): Bundle{
            val args = Bundle()
            args.putString(USER_NAME_KEY, userName)
            return args
        }
    }

}
