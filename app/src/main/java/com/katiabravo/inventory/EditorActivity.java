package com.katiabravo.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.katiabravo.inventory.data.ProductContract.ProductEntry;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final int EXISTING_PRODUCT_LOADER = 0;
    private Uri mCurrentProductUri;

    private EditText mProductEditText;
    private static TextView mQuantityTextView;
    private Button mAddQuantityButton;
    private Button mSubtractQuantityButton;
    private EditText mPriceEditText;

    private boolean mProductHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_product_detail));
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        mProductEditText = (EditText) findViewById(R.id.edit_product_name);
        mQuantityTextView = (TextView) findViewById(R.id.edit_product_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mAddQuantityButton = (Button) findViewById(R.id.add_quantity);
        mSubtractQuantityButton = (Button) findViewById(R.id.subtract_quantity);

        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityTextView.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mAddQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addOneToQuantity();
                mProductHasChanged = true;
            }
        });

        mSubtractQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                subtractOneToQuantity();
                mProductHasChanged = true;
            }
        });
    }

    private void addOneToQuantity() {
        String previousQuantityString = mQuantityTextView.getText().toString();
        int previousQuantity;
        if (previousQuantityString.isEmpty()) {
            previousQuantity = 0;
        } else {
            previousQuantity = Integer.parseInt(previousQuantityString);
        }
        mQuantityTextView.setText(String.valueOf(previousQuantity + 5));
    }

    public static void subtractOneToQuantity() {
        String previousQuantityString = mQuantityTextView.getText().toString();
        int previousQuantity;
        if (previousQuantityString.isEmpty()) {
            previousQuantity = 0;
        } else {
            previousQuantity = Integer.parseInt(previousQuantityString);
        }
        if (previousQuantity == 0){
            mQuantityTextView.setText(String.valueOf(previousQuantity));
        }else{
            mQuantityTextView.setText(String.valueOf(previousQuantity - 5));
        }
    }

    private void saveProduct() {
        String productNameString = mProductEditText.getText().toString().trim();
        String quantityString = mQuantityTextView.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();

        if (mCurrentProductUri == null && TextUtils.isEmpty(productNameString) && quantityString == "0"
                && TextUtils.isEmpty(priceString)) {return;}

        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, productNameString);
        values.put(ProductEntry.COLUMN_QUANTITY, quantityString);
        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
        }
        values.put(ProductEntry.COLUMN_PRICE, price);

        Uri newUri;
        if (mCurrentProductUri == null) {
            newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }else{
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values,null, null);

            if(rowsAffected == 0){
                Toast.makeText(this, getString(R.string.editor_update_product_failed), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, getString(R.string.editor_update_product_successful), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showOrderConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.order_message);
        builder.setPositiveButton(R.string.email, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(android.content.Intent.ACTION_SENDTO);
                intent.setType("text/plain");
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Recurrent new order");
                String bodyMessage = "Please send us more " + mProductEditText.getText().toString().toLowerCase().trim() + "s as soon as possible!";
                intent.putExtra(android.content.Intent.EXTRA_TEXT, bodyMessage);
                startActivity(intent);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentProductUri == null) {
            MenuItem deleteMenuItem = menu.findItem(R.id.action_delete);
            deleteMenuItem.setVisible(false);
            MenuItem orderMoreItem = menu.findItem(R.id.action_order_more);
            orderMoreItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveProduct();
                finish();
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case R.id.action_order_more:
                showOrderConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!mProductHasChanged){
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return  true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null){
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_product_failed), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] project = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_QUANTITY,
                ProductEntry.COLUMN_PRICE
        };

        return new CursorLoader(this,
                mCurrentProductUri,
                project,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if(cursor.moveToFirst()){
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRICE);

            String name = cursor.getString(nameColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);

            mProductEditText.setText(name);
            mQuantityTextView.setText(Integer.toString(quantity));
            mPriceEditText.setText(Integer.toString(price));

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProductEditText = (EditText) findViewById(R.id.edit_product_name);
        mQuantityTextView = (TextView) findViewById(R.id.edit_product_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
