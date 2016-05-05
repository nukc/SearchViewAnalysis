# SearchView源码解析

SearchView是一个搜索框控件，样式也挺好看的。这次解析主要围绕`android.support.v7.widget`包下的SearchView（API >= 7）,`android.widget.SearchView`支持API >= 11，
另外有个`android.support.v4.widget.SearchViewCompat`。

## 目录

- <a href="#analysis">源码解析</a>
	- <a href="#extends">1. 继承关系</a>
	- <a href="#widgets">2. 主要组件</a>
	- <a href="#construct">3. 构造方法和自定义</a>
	- <a href="#listener">4. Listener</a>
	- <a href="#collapsibleactionview">5. CollapsibleActionView接口</a>
	- <a href="#instancestate">6. 状态的保存和恢复</a>
	- <a href="#suggestions">7. 关于Suggestions和Searchable</a>
	- <a href="#voice">8. 语音搜索功能</a>
	- <a href="#reflector">9. AutoCompleteTextViewReflector</a>
	- <a href="#onmeasure">10. onMeasure 测量</a>

## <div id="analysis">源码解析</div>

v7版本：23.2.1

#### <div id="extends">1. 继承关系</div>

<table>
   <tbody>
        <tr>
	    <td colspan="5">java.lang.Object</td>
        </tr>
        <tr>
	    <td>&nbsp;&nbsp;&nbsp;↳</td>
	    <td colspan="4">android.view.View</td>
        </tr>
	<tr>
            <td>&nbsp;</td>
            <td>&nbsp;&nbsp;&nbsp;↳</td>
            <td colspan="3">android.view.ViewGroup</a></td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
            <td>&nbsp;&nbsp;&nbsp;↳</td>
            <td colspan="2">android.support.v7.widget.LinearLayoutCompat</td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
            <td>&nbsp;&nbsp;&nbsp;↳</td>
            <td colspan="1">android.support.v7.widget.SearchView</td>
        </tr>
	</tbody>
</table>

#### <div id="widgets">2. 主要组件</div>

```java

    private final SearchAutoComplete mSearchSrcTextView;
    private final View mSearchEditFrame;
    private final View mSearchPlate;
    private final View mSubmitArea;
    private final ImageView mSearchButton;
    private final ImageView mGoButton;
    private final ImageView mCloseButton;
    private final ImageView mVoiceButton;
    private final View mDropDownAnchor;
    private final ImageView mCollapsedIcon;

```
看命名也能大概知道控件各自充当了什么角色了。

#### <div id="construct">3. 构造方法和自定义</div>

接下来看构造方法`public SearchView(Context context, AttributeSet attrs, int defStyleAttr)`,`v7`的`SearchView`并不是用`TypedArray`而是使用`TintTypedArray`，看了源码发现`TintTypedArray`里有个：``` private final TypedArray mWrapped; ```所以主要还是`TypedArray`，不同点是`getDrawable(int index)`和新加的`getDrawableIfKnown(int index)`方法，
并在满足条件下会调用`AppCompatDrawableManager.get().getDrawable(mContext, resourceId)`。

为了能更好的自定义，`SearchView`的layout也是可以指定的，不过自定义的layout必须包括上面那些控件，同时id也是指定的，
不然后面会报错，因为`findViewById(id)`无法找到各自控件，然后调用控件方法的时候就。。。

构造方法最后是更新控件状态，`mIconifiedByDefault`默认是`true`的，`setIconifiedByDefault(boolean iconified)`改变值后也会执行如下方法：

```java
    public void setIconifiedByDefault(boolean iconified) {
        if (mIconifiedByDefault == iconified) return;
        mIconifiedByDefault = iconified;
        //更新组件
        updateViewsVisibility(iconified);
        updateQueryHint();
    }
```

所以`setIconifiedByDefault(false)`会让SearchView一直呈现展开状态，并且输入框内icon也会不显示。具体方法如下，该方法在`updateQueryHint()`中被调用：
```java

    private CharSequence getDecoratedHint(CharSequence hintText) {
        //如果mIconifiedByDefault为false或者mSearchHintIcon为null
        //将不会添加搜索icon到提示hint中
        if (!mIconifiedByDefault || mSearchHintIcon == null) {
            return hintText;
        }

        final int textSize = (int) (mSearchSrcTextView.getTextSize() * 1.25);
        mSearchHintIcon.setBounds(0, 0, textSize, textSize);

        final SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
        ssb.setSpan(new ImageSpan(mSearchHintIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(hintText);
        return ssb;
    }

```

#### <div id="listener">4. Listener</div>

然后，我们来看看`SearchView`里面有哪些Listener：

```java

    //里面有2个方法：
        //onQueryTextSubmit(String query)：当用户提交查询的时候会调用
        //onQueryTextChange(String newText)：当查询文字改变的时候会调用
    private OnQueryTextListener mOnQueryChangeListener;
    
    //里面有1个方法：boolean onClose();
        //onClose()：当mCloseButton被点击和setIconified(true)会判断是否调用
        //是否调用是在onCloseClicked()里判断，后面会进行分析 
    private OnCloseListener mOnCloseListener;
    
    //View类里定义的接口
    private OnFocusChangeListener mOnQueryTextFocusChangeListener;
    
    //里面有2个方法：
        //onSuggestionSelect(int position)：选择建议可选项（搜索框下方出现的）后触发
        //onSuggestionClick(int position)：点击建议可选项后触发
    private OnSuggestionListener mOnSuggestionListener;
    
    //View类里定义的接口
    private OnClickListener mOnSearchClickListener;

    //还有其他mOnClickListener，mTextKeyListener等
```

我们看看OnQueryTextListener是怎样进行监听的：

- onQueryTextChange(String newText)

```java
    //在构造方法里添加了监听
    mSearchSrcTextView.addTextChangedListener(mTextWatcher);
```
然后在`mTextWatcher`的`onTextChanged()`方法里调用了SearchView的`onTextChanged(CharSequence newText)`方法，
也就是在这里进行了判断触发:

```java

    private void onTextChanged(CharSequence newText) {
        /**
         * 省略代码,主要是更新组件
         */

        //当listener!=null和当文本不一样的时候会触发。
        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }

        //省略代码
    }

```

- onQueryTextSubmit(String query)

```java

    //同在构造方法里添加了监听
    mSearchSrcTextView.setOnEditorActionListener(mOnEditorActionListener);

    private final OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {
    
        /**
         * Called when the input method default action key is pressed.
         */
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            onSubmitQuery();
            return true;
        }
    };

    private void onSubmitQuery() {
        CharSequence query = mSearchSrcTextView.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            
            //当监听OnQueryChangeListener了之后，
            //当onQueryTextSubmit() return true的话，是不会执行下面操作的
            if (mOnQueryChangeListener == null
                    || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                    
                //设置了Searchable后，会startActivity到配置指定的Activity    
                if (mSearchable != null) {
                    launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, query.toString());
                }
                //设置键盘是否显示
                setImeVisibility(false); 
                
                //下拉可选项是用ListPopupWindow显示的，具体可看 AutoCompleteTextView 源码
                //搜索提交后，dismiss后就不会继续显示而挡住内容什么的
                dismissSuggestions();
            }
        }
    }
```

在if里加入`!mOnQueryChangeListener.onQueryTextSubmit(query.toString())`，这样做就可以让使用者自己决定是否完全自己处理,
灵活性也更高。

其他Listener差不多也是这样，那接下来看看其他的。

#### <div id="collapsibleactionview">5. CollapsibleActionView接口</div>

SearchView实现了CollapsibleActionView接口：onActionViewExpanded()和onActionViewCollapsed(),具体操作就是
设置键盘及控件，并使用全局变量`mExpandedInActionView`记录ActionView是否伸展。只有当SearchView作为MenuItem的时候
才会触发，如果是使用v7包的话，想要通过menu获取SearchView就需要使用MenuItemCompat类，具体可以看demo。
```java
    MenuItemCompat.getActionView(android.view.MenuItem item);
```

#### <div id="instancestate">6. 状态的保存和恢复</div>

SearchView覆写了onSaveInstanceState()和onRestoreInstanceState(Parcelable state)用来保存和恢复状态，为什么要覆写呢？
因为需要额外保存`boolean mIconified`，为此还建了个内部静态类SavedState用来保存mIconified。

```java

    //实现了Parcelable序列化
    static class SavedState extends BaseSavedState {
        boolean isIconified;

        /**
         * 省略其他代码
         */
    }

```

#### <div id="suggestions">7. 关于Suggestions和Searchable</div>

如果你使用了Suggestions，而且没有setSearchableInfo，那么当你点击建议可选项的时候会log：

```java
W/SearchView: Search suggestions cursor at row 0 returned exception.
              java.lang.NullPointerException
                  at android.support.v7.widget.SearchView.createIntentFromSuggestion(SearchView.java:1620)
                  at android.support.v7.widget.SearchView.launchSuggestion(SearchView.java:1436)
                  at android.support.v7.widget.SearchView.onItemClicked(SearchView.java:1349)
                  at android.support.v7.widget.SearchView.access$1800(SearchView.java:103)
                  at android.support.v7.widget.SearchView$10.onItemClick(SearchView.java:1373)
                  ......
```

定位到第1620行：

```java
    private Intent createIntentFromSuggestion(Cursor c, int actionKey, String actionMsg) {
        try {

            // use specific action if supplied, or default action if supplied, or fixed default
            String action = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_ACTION);

            //在这里并没有检查mSearchable是否为null
            if (action == null && Build.VERSION.SDK_INT >= 8) {
                action = mSearchable.getSuggestIntentAction();  //第1620行
            }

            /**
             *省略部分代码
             */

            return createIntent(action, dataUri, extraData, query, actionKey, actionMsg);
        } catch (RuntimeException e ) {

            /**
             *省略部分代码
             */

            Log.w(LOG_TAG, "Search suggestions cursor at row " + rowNum +
                                    " returned exception.", e);
            return null;
        }
    }
```

发现调用mSearchable的方法之前并没有检查mSearchable是否为null，其他地方是有判断的，由于做了catch所以不会crash，
也不影响使用，另外，如果setOnSuggestionListener：

```java
    mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return true; //返回true
        }
    });
```

onSuggestionClick(int position) 返回 true 就不会执行`createIntentFromSuggestion(~)`，
也就不会log了，但这样，键盘的隐藏和可选项pop的dismiss也不会执行，需要自己处理，使用SearchView的`clearFocus()`方法就能达到同样的效果。

那既然是报null，那就设置Searchable吧，设置后是会startActivity的(执行完createIntentFromSuggestion(~)后就会执行)。
然后效果就是当你点击了可选项就会startActivity，看需求做选择吧。。

#### <div id="voice">8. 语音搜索功能</div>

SearchView还有语音搜索功能(API >= 8)，需要通过配置Searchable来开启，在xml配置文件中加入：

```xml
android:voiceSearchMode="showVoiceSearchButton|launchRecognizer"
```

`showVoiceSearchButton`显示语音搜索按钮，`launchRecognizer`表示要启动一个语音识别器来转换成文字传给指定的searchable activity。
有个全局变量`boolean mVoiceButtonEnabled`表示是否启用，在`setSearchableInfo(~)`方法里进行了设置：

```java
mVoiceButtonEnabled = IS_AT_LEAST_FROYO && hasVoiceSearch();
```

IS_AT_LEAST_FROYO是Build.VERSION.SDK_INT >= 8，为了确保正确性，我试了下，结果并没有显示语言搜索按钮，
debug后发现在hasVoiceSearch()里：

```java
    ResolveInfo ri = getContext().getPackageManager().resolveActivity(testIntent,
            PackageManager.MATCH_DEFAULT_ONLY);
    return ri != null;
```

在这里并没有resolve到Activity，结果return false，mVoiceButtonEnabled也就变成false了。(┙>∧<)┙へ┻┻

终于知道为什么了，原来阉割版的系统都不会出现语音搜索按钮，华为/魅族/Genymotion试过都不行(没有试过全版本系统),
AS自带模拟器可以(有Google服务)，具体应该就是没有resolve到Google语音识别Activity。对语音识别有兴趣的同学可以搜索RecognizerIntent。

#### <div id="reflector">9. AutoCompleteTextViewReflector</div>

v7包的SearchView使用了反射机制，通过反射拿到AutoCompleteTextView和InputMethodManager隐藏的方法。

```java

    static final AutoCompleteTextViewReflector HIDDEN_METHOD_INVOKER = new AutoCompleteTextViewReflector();

    private static class AutoCompleteTextViewReflector {
        private Method doBeforeTextChanged, doAfterTextChanged;
        private Method ensureImeVisible;
        private Method showSoftInputUnchecked;

        AutoCompleteTextViewReflector() {
            
            /**
             * 省略部分代码
             */
            
            try {
                showSoftInputUnchecked = InputMethodManager.class.getMethod(
                        "showSoftInputUnchecked", int.class, ResultReceiver.class);
                showSoftInputUnchecked.setAccessible(true);
            } catch (NoSuchMethodException e) {
                // Ah well.
            }
        }    
    
        /**
         * 省略部分代码
         */        
    
        void showSoftInputUnchecked(InputMethodManager imm, View view, int flags) {
            if (showSoftInputUnchecked != null) {
                try {
                    showSoftInputUnchecked.invoke(imm, flags, null);
                    return;
                } catch (Exception e) {
                }
            }

            //只有这个方法才有在if后面做处理
            // Hidden method failed, call public version instead
            imm.showSoftInput(view, flags);
        }        
    }

```

#### <div id="onmeasure">10. onMeasure 测量</div>

查看了下`onMeasure`, 发现有个地方还是比较在意的. 当`isIconified()`返回`false`的时候, width的mode在最后都会被设置成MeasureSpec.EXACTLY.
在SearchView伸展收缩的时候, `onMeasure`会被执行多次, width根据其mode改变, 之后mode设置为EXACTLY再调用父类super方法进行测量.
设置为EXACTLY, 这样父控件就能确切的决定view的大小, 那为什么只对width而不对height进行设置呢?
通过查看默认的[layout](https://github.com/nukc/SearchViewAnalysis/blob/master/app%2Fsrc%2Fmain%2Fres%2Flayout%2Flayout_search.xml),
可以看到主要组件的layout_height的大多都是match_parent(对应EXACTLY模式), 而layout_width基本都是wrap_content(对应AT_MOST模式).
另外, 不是只有伸展收缩的时候, `onMeasure`才会被执行, 点击语音搜索按钮/输入框获取焦点的时候/...也会执行.

```java

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Let the standard measurements take effect in iconified state.
        if (isIconified()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        switch (widthMode) {
            case MeasureSpec.AT_MOST:
                // If there is an upper limit, don't exceed maximum width (explicit or implicit)
                if (mMaxWidth > 0) {
                    width = Math.min(mMaxWidth, width);
                } else {
                    width = Math.min(getPreferredWidth(), width);
                }
                break;
            case MeasureSpec.EXACTLY:
                // If an exact width is specified, still don't exceed any specified maximum width
                if (mMaxWidth > 0) {
                    width = Math.min(mMaxWidth, width);
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                // Use maximum width, if specified, else preferred width
                width = mMaxWidth > 0 ? mMaxWidth : getPreferredWidth();
                break;
        }
        widthMode = MeasureSpec.EXACTLY;
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode), heightMeasureSpec);
    }

```

未完，待续。。。。

如果我哪里分析错了，请大家及时纠正我，谢谢。:)
