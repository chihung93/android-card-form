package com.braintreepayments.cardform.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.widget.TextViewCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;

import com.braintreepayments.cardform.R;
import com.braintreepayments.cardform.utils.CardNumberTransformation;
import com.braintreepayments.cardform.utils.CardType;

/**
 * An {@link android.widget.EditText} that displays Card icons based on the number entered.
 */
public class CardEditText extends ErrorEditText implements TextWatcher {

    public interface OnCardTypeChangedListener {
        void onCardTypeChanged(CardType cardType);
    }

    private boolean mDisplayCardIcon = true;
    private boolean mMask = false;
    private CardType mCardType;
    private OnCardTypeChangedListener mOnCardTypeChangedListener;
    private TransformationMethod mSavedTranformationMethod;
    private String origin = "";
    private static final String digitDots = "••••••••••••";
    private boolean ignoreFocus = false;
    public CardEditText(Context context) {
        super(context);
        init();
    }

    public CardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CardEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_CLASS_NUMBER);
        setCardIcon(R.drawable.bt_ic_unknown);
        addTextChangedListener(this);
        updateCardType();
        mSavedTranformationMethod = getTransformationMethod();
    }

    /**
     * Enable or disable showing card type icons as part of the {@link CardEditText}. Defaults to
     * {@code true}.
     *
     * @param display {@code true} to display card type icons, {@code false} to never display card
     *                type icons.
     */
    public void displayCardTypeIcon(boolean display) {
        mDisplayCardIcon = display;

        if (!mDisplayCardIcon) {
            setCardIcon(-1);
        }
    }

    /**
     * @return The {@link com.braintreepayments.cardform.utils.CardType} currently entered in
     * the {@link android.widget.EditText}
     */
    public CardType getCardType() {
        return mCardType;
    }

    /**
     * @param mask if {@code true}, all but the last four digits of the card number will be masked when
     *             focus leaves the card field. Uses {@link CardNumberTransformation}, transforming the number from
     *             something like "4111111111111111" to "•••• 1111".
     */
    public void setMask(boolean mask) {
        mMask = mask;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (ignoreFocus){
            return;
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            unmaskNumber();

            if (getText().toString().length() > 0) {
                setSelection(getText().toString().length());
            }
        } else if (isValid()) {
            maskNumber();
        }
    }

    /**
     * Receive a callback when the {@link com.braintreepayments.cardform.utils.CardType} changes
     *
     * @param listener to be called when the {@link com.braintreepayments.cardform.utils.CardType}
     *                 changes
     */
    public void setOnCardTypeChangedListener(OnCardTypeChangedListener listener) {
        mOnCardTypeChangedListener = listener;
    }

    @Override
    public void afterTextChanged(Editable editable) {
        Object[] paddingSpans = editable.getSpans(0, editable.length(), SpaceSpan.class);
        for (Object span : paddingSpans) {
            editable.removeSpan(span);
        }

        updateCardType();
        setCardIcon(mCardType.getFrontResource());

        if (!editable.toString().contains(digitDots)){
            origin = editable.toString();
        }

        if (mCardType.getMaxCardLength() == getSelectionStart()) {
            validate();
            if (isValid()) {
                if (ignoreFocus){
                    return;
                }
                focusNextView();
            } else {
                unmaskNumber();
            }
        } else if (!hasFocus()) {
            maskNumber();
        }

        addSpans(editable, mCardType.getSpaceIndices());
    }

    @Override
    public boolean isValid() {
        return isOptional() || mCardType.validate(getText().toString());
    }

    @Override
    public String getErrorMessage() {
        if (TextUtils.isEmpty(getText())) {
            return getContext().getString(R.string.bt_card_number_required);
        } else {
            return getContext().getString(R.string.bt_card_number_invalid);
        }
    }

    private void maskNumber() {
        if (mMask && !(getTransformationMethod() instanceof CardNumberTransformation)) {
            mSavedTranformationMethod = getTransformationMethod();

            setTransformationMethod(new CardNumberTransformation());
        } else {
            ignoreFocus = true;
            removeTextChangedListener(this);
            setText(digitDots + origin.subSequence(origin.length() - 4, origin.length()));
            addSpans(getEditableText(), mCardType.getSpaceIndices());
            addTextChangedListener(this);
            ignoreFocus = false;
        }
    }

    private void unmaskNumber() {
        if (getTransformationMethod() != mSavedTranformationMethod) {
            setTransformationMethod(mSavedTranformationMethod);
        }
        if (!TextUtils.isEmpty(origin)) {

            removeTextChangedListener(this);
            setText(origin);
            addSpans(getEditableText(), mCardType.getSpaceIndices());
            addTextChangedListener(this);
        }
    }

    private void updateCardType() {
        CardType type = CardType.forCardNumber(getText().toString());
        if (mCardType != type) {
            mCardType = type;

            InputFilter[] filters = {new LengthFilter(mCardType.getMaxCardLength())};
            setFilters(filters);
            invalidate();

            if (mOnCardTypeChangedListener != null) {
                mOnCardTypeChangedListener.onCardTypeChanged(mCardType);
            }
        }
    }

    private void addSpans(Editable editable, int[] spaceIndices) {
        final int length = editable.length();
        for (int i = 0; i < spaceIndices.length; i++) {
            int index = spaceIndices[i];
            if (index <= length) {
                if (i == spaceIndices.length - 1) {
                    editable.setSpan(new SpaceSpanLong(), index - 1, index,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    editable.setSpan(new SpaceSpan(), index - 1, index,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private void setCardIcon(int icon) {
        if (!mDisplayCardIcon || getText().length() == 0) {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(this, 0, 0, 0, 0);
        } else {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(this, 0, 0, icon, 0);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }
}
