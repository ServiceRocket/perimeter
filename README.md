# Perimeter

This add-on allows potential view access into pages which would otherwise be inaccessible. However, it does not allow any access which would not be possible by someone who could view the inaccessible page simply copying and pasting the page contents into another space. That is, if you trust people to view a page in a restricted space, then this does not give them any further power.

## Secure-include Macro

This macro makes it possible to include a page from another space which may be inaccessible to all viewers of the current space.

It does *not* allow arbitrary including of pages from other spaces. The only users who can include a page from a secure space are those who already have view access into the other space. If that user's access is later revoked, the include ceases to function.

So, when is this useful? The main case is when there is a page inside a secured space which is of interest to a wider audience. It may be argued that the page should be moved elsewhere, but in some cases this isn't practical, or even possible.

Alternately, the content could be copy and pasted into a page in a more 'open' space. however, then it will become out-of-date over time.

### Parameters

| Name | Required | Default Value | Description |
|------|----------|---------------|-------------|
|id    |   Yes    |               |The id which is unique to the current page. This allows multiple includes on a single page. It does not have to be related to the page being included.|

## Wiki Markup
```
{secure-include:id=[unique id]}
```

### Notes
After creating the macro and saving the page, you will now see a panel very much like in the following image:

![](https://docs.servicerocket.com/download/attachments/8487005/link.png?version=1&modificationDate=1431674786906&api=v2)

Despite the text requesting a link to the page, enter the **page title** you intend to include instead. If the page is on another space, use the SPACE: PAGE format.